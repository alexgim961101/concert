import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';

/**
 * 대기열 동시성 테스트
 * 
 * 동일한 콘서트에 대해 대량의 사용자가 동시에 토큰을 요청할 때의 동작을 검증합니다.
 * - 최대 50개의 ACTIVE 토큰 제한이 적용되는지 확인
 * - 나머지 사용자는 WAITING 상태가 되는지 확인
 * - 토큰 활성화 스케줄러가 제대로 동작하는지 확인
 */

// Metrics
const activeTokens = new Counter('active_tokens');
const waitingTokens = new Counter('waiting_tokens');
const tokenActivationTime = new Trend('token_activation_time', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CONCERT_ID = __ENV.CONCERT_ID || 1;

export const options = {
    scenarios: {
        // 동시 토큰 요청 테스트
        concurrent_token_requests: {
            executor: 'shared-iterations',
            vus: 100,
            iterations: 200,
            maxDuration: '2m',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const userId = __VU * 1000 + __ITER;

    // 1. 토큰 발급
    const tokenPayload = JSON.stringify({
        userId: userId,
        concertId: CONCERT_ID,
    });

    const tokenRes = http.post(`${BASE_URL}/api/v1/queue/tokens`, tokenPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    const tokenSuccess = check(tokenRes, {
        'token issued': (r) => r.status === 200,
    });

    if (!tokenSuccess) {
        console.log(`Token issue failed: ${tokenRes.status} - ${tokenRes.body}`);
        return;
    }

    const data = JSON.parse(tokenRes.body).data;
    const token = data.token;
    const initialStatus = data.status;

    if (initialStatus === 'ACTIVE') {
        activeTokens.add(1);
        console.log(`User ${userId}: Immediately ACTIVE`);
    } else {
        waitingTokens.add(1);

        // WAITING이면 ACTIVE가 될 때까지 대기
        const startWait = Date.now();
        let activated = false;

        for (let i = 0; i < 60; i++) {
            sleep(1);

            const statusRes = http.get(`${BASE_URL}/api/v1/queue/status`, {
                headers: { 'Concert-Queue-Token': token },
            });

            if (statusRes.status === 200) {
                const statusData = JSON.parse(statusRes.body).data;
                if (statusData.status === 'ACTIVE') {
                    tokenActivationTime.add(Date.now() - startWait);
                    console.log(`User ${userId}: Activated after ${(Date.now() - startWait) / 1000}s`);
                    activated = true;
                    break;
                }
            }
        }

        if (!activated) {
            console.log(`User ${userId}: Timed out waiting for activation`);
        }
    }

    sleep(1);
}

export function handleSummary(data) {
    const metrics = data.metrics;

    return {
        stdout: `
╔════════════════════════════════════════════════════════════════╗
║              Queue Concurrency Test Summary                    ║
╚════════════════════════════════════════════════════════════════╝

📊 Token Distribution:
   • Immediately ACTIVE: ${metrics.active_tokens?.values?.count || 0}
   • Initially WAITING:  ${metrics.waiting_tokens?.values?.count || 0}

⏱️ Activation Time (for WAITING tokens):
   • Average: ${(metrics.token_activation_time?.values?.avg / 1000 || 0).toFixed(2)}s
   • P95:     ${(metrics.token_activation_time?.values?.['p(95)'] / 1000 || 0).toFixed(2)}s
   • Max:     ${(metrics.token_activation_time?.values?.max / 1000 || 0).toFixed(2)}s

📈 HTTP Metrics:
   • Total Requests: ${metrics.http_reqs?.values?.count || 0}
   • Failed Rate:    ${((metrics.http_req_failed?.values?.rate || 0) * 100).toFixed(2)}%
   • P95 Duration:   ${(metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms
`,
        'k6/results/queue-concurrency.json': JSON.stringify(data, null, 2),
    };
}
