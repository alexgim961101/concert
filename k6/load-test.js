import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';

// Custom metrics
const tokenIssueDuration = new Trend('token_issue_duration', true);
const tokenStatusDuration = new Trend('token_status_duration', true);
const reservationDuration = new Trend('reservation_duration', true);
const paymentDuration = new Trend('payment_duration', true);
const failedRequests = new Counter('failed_requests');
const successRate = new Rate('success_rate');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    scenarios: {
        // 시나리오 1: 부하 테스트 (점진적 증가)
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },   // Ramp up to 50 users
                { duration: '1m', target: 50 },    // Stay at 50 users
                { duration: '30s', target: 100 },  // Ramp up to 100 users
                { duration: '1m', target: 100 },   // Stay at 100 users
                { duration: '30s', target: 0 },    // Ramp down
            ],
            exec: 'concertReservationFlow',
            startTime: '0s',
        },
        // 시나리오 2: 스파이크 테스트 (티켓팅 오픈 시뮬레이션)
        spike_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 10 },   // Warm up
                { duration: '1s', target: 200 },   // Spike! (티켓팅 오픈)
                { duration: '30s', target: 200 },  // Maintain spike
                { duration: '10s', target: 10 },   // Recovery
            ],
            exec: 'tokenIssueOnly',
            startTime: '4m',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95% < 500ms, 99% < 1s
        http_req_failed: ['rate<0.01'],                  // 에러율 1% 미만
        success_rate: ['rate>0.99'],                     // 성공률 99% 이상
        token_issue_duration: ['p(95)<300'],             // 토큰 발급 95% < 300ms
        token_status_duration: ['p(95)<100'],            // 토큰 상태 조회 95% < 100ms
    },
};

// 헬퍼 함수: API 호출 결과 체크
function checkResponse(res, name) {
    const success = check(res, {
        [`${name}: status is 200`]: (r) => r.status === 200,
        [`${name}: response has data`]: (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.success === true && body.data !== undefined;
            } catch {
                return false;
            }
        },
    });

    if (!success) {
        failedRequests.add(1);
        console.log(`${name} failed: ${res.status} - ${res.body}`);
    }
    successRate.add(success);
    return success;
}

// 시나리오 함수: 전체 콘서트 예약 플로우
export function concertReservationFlow() {
    const userId = __VU * 1000 + __ITER;
    const concertId = 1;

    group('1. Token Issue', () => {
        const payload = JSON.stringify({
            userId: userId,
            concertId: concertId,
        });

        const startTime = Date.now();
        const res = http.post(`${BASE_URL}/api/v1/queue/tokens`, payload, {
            headers: { 'Content-Type': 'application/json' },
        });
        tokenIssueDuration.add(Date.now() - startTime);

        if (!checkResponse(res, 'Token Issue')) {
            return;
        }

        const data = JSON.parse(res.body).data;
        const token = data.token;

        // 토큰 상태가 WAITING이면 대기
        if (data.status === 'WAITING') {
            group('1.1 Wait for Active', () => {
                let attempts = 0;
                const maxAttempts = 30;

                while (attempts < maxAttempts) {
                    sleep(1);
                    const startTime = Date.now();
                    const statusRes = http.get(`${BASE_URL}/api/v1/queue/status`, {
                        headers: { 'Concert-Queue-Token': token },
                    });
                    tokenStatusDuration.add(Date.now() - startTime);

                    if (statusRes.status === 200) {
                        const statusData = JSON.parse(statusRes.body).data;
                        if (statusData.status === 'ACTIVE') {
                            break;
                        }
                    }
                    attempts++;
                }
            });
        }

        // 2. 좌석 예약
        group('2. Reserve Seat', () => {
            const seatPayload = JSON.stringify({
                userId: userId,
                scheduleId: 1,
                seatNumber: (userId % 50) + 1,
            });

            const startTime = Date.now();
            const reserveRes = http.post(`${BASE_URL}/api/v1/reservations`, seatPayload, {
                headers: {
                    'Content-Type': 'application/json',
                    'Concert-Queue-Token': token,
                },
            });
            reservationDuration.add(Date.now() - startTime);

            // 좌석이 이미 예약된 경우 409 CONFLICT도 정상 케이스로 간주
            if (reserveRes.status !== 200 && reserveRes.status !== 409) {
                failedRequests.add(1);
                successRate.add(false);
                console.log(`Reserve failed: ${reserveRes.status} - ${reserveRes.body}`);
                return;
            }
            successRate.add(true);

            if (reserveRes.status === 200) {
                const reservationData = JSON.parse(reserveRes.body).data;

                // 3. 결제
                group('3. Payment', () => {
                    const paymentPayload = JSON.stringify({
                        reservationId: reservationData.reservationId,
                        userId: userId,
                    });

                    const startTime = Date.now();
                    const paymentRes = http.post(`${BASE_URL}/api/v1/payments`, paymentPayload, {
                        headers: {
                            'Content-Type': 'application/json',
                            'Concert-Queue-Token': token,
                        },
                    });
                    paymentDuration.add(Date.now() - startTime);

                    checkResponse(paymentRes, 'Payment');
                });
            }
        });
    });

    sleep(1);
}

// 시나리오 함수: 토큰 발급만 (스파이크 테스트용)
export function tokenIssueOnly() {
    const userId = __VU * 10000 + __ITER;
    const concertId = 1;

    const payload = JSON.stringify({
        userId: userId,
        concertId: concertId,
    });

    const startTime = Date.now();
    const res = http.post(`${BASE_URL}/api/v1/queue/tokens`, payload, {
        headers: { 'Content-Type': 'application/json' },
    });
    tokenIssueDuration.add(Date.now() - startTime);

    checkResponse(res, 'Token Issue (Spike)');

    sleep(0.1);
}

// Summary 핸들러
export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'k6/results/summary.json': JSON.stringify(data, null, 2),
    };
}

// 텍스트 서머리 (간략 버전)
function textSummary(data, opts) {
    const metrics = data.metrics;
    const checks = data.root_group.checks;

    let summary = `
╔════════════════════════════════════════════════════════════════╗
║                    k6 Performance Test Summary                 ║
╚════════════════════════════════════════════════════════════════╝

📊 HTTP Request Metrics:
   • Total Requests: ${metrics.http_reqs?.values?.count || 0}
   • Avg Duration:   ${(metrics.http_req_duration?.values?.avg || 0).toFixed(2)}ms
   • P95 Duration:   ${(metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms
   • P99 Duration:   ${(metrics.http_req_duration?.values?.['p(99)'] || 0).toFixed(2)}ms
   • Failed Rate:    ${((metrics.http_req_failed?.values?.rate || 0) * 100).toFixed(2)}%

📈 Custom Metrics:
   • Token Issue P95:     ${(metrics.token_issue_duration?.values?.['p(95)'] || 0).toFixed(2)}ms
   • Token Status P95:    ${(metrics.token_status_duration?.values?.['p(95)'] || 0).toFixed(2)}ms
   • Reservation P95:     ${(metrics.reservation_duration?.values?.['p(95)'] || 0).toFixed(2)}ms
   • Payment P95:         ${(metrics.payment_duration?.values?.['p(95)'] || 0).toFixed(2)}ms

✅ Checks:
`;

    if (checks) {
        Object.values(checks).forEach(check => {
            const passed = check.passes;
            const failed = check.fails;
            const rate = ((passed / (passed + failed)) * 100).toFixed(1);
            summary += `   • ${check.name}: ${rate}% (${passed}/${passed + failed})\n`;
        });
    }

    summary += `
🚦 Thresholds:
`;
    if (data.thresholds) {
        Object.entries(data.thresholds).forEach(([name, threshold]) => {
            const status = threshold.ok ? '✅' : '❌';
            summary += `   ${status} ${name}\n`;
        });
    }

    return summary;
}
