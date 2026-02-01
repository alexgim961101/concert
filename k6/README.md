# k6 부하 테스트

Concert Reservation Service의 성능 및 부하 테스트를 위한 k6 스크립트입니다.

## 사전 요구사항

### k6 설치 (macOS)

```bash
brew install k6
```

### 애플리케이션 실행

```bash
# Docker Compose로 인프라 실행
docker compose up -d

# 애플리케이션 실행
./gradlew bootRun
```

## 테스트 스크립트

### 1. 전체 부하 테스트 (`load-test.js`)

전체 콘서트 예약 플로우(토큰 발급 → 좌석 예약 → 결제)를 테스트합니다.

```bash
k6 run k6/load-test.js
```

**시나리오:**
- **Load Test**: 50 → 100 VU 점진적 증가 (약 4분)
- **Spike Test**: 10 → 200 VU 스파이크 (티켓팅 오픈 시뮬레이션)

**Thresholds:**
- HTTP 요청 95% < 500ms
- HTTP 요청 99% < 1000ms
- 에러율 < 1%
- 토큰 발급 95% < 300ms

### 2. 대기열 동시성 테스트 (`queue-concurrency-test.js`)

대량 사용자가 동시에 토큰을 요청할 때 대기열 시스템의 동작을 검증합니다.

```bash
k6 run k6/queue-concurrency-test.js
```

**검증 포인트:**
- 최대 50개의 ACTIVE 토큰 제한
- WAITING → ACTIVE 전환 시간
- 토큰 활성화 스케줄러 동작

## 환경 변수

```bash
# 다른 서버 URL 지정
k6 run -e BASE_URL=http://your-server:8080 k6/load-test.js

# 특정 콘서트 ID로 테스트
k6 run -e CONCERT_ID=2 k6/queue-concurrency-test.js
```

## 테스트 결과

결과는 `k6/results/` 디렉토리에 JSON 형식으로 저장됩니다:
- `summary.json`: 전체 부하 테스트 결과
- `queue-concurrency.json`: 대기열 동시성 테스트 결과

## 주의사항

1. **테스트 데이터**: 테스트 전에 적절한 시드 데이터가 필요합니다.
   ```bash
   ./scripts/seed.sh
   ```

2. **리소스**: 대규모 테스트 시 로컬 머신의 파일 디스크립터 제한을 늘리세요.
   ```bash
   ulimit -n 10000
   ```

3. **DB 정리**: 테스트 후 데이터 정리가 필요할 수 있습니다.
   ```bash
   ./scripts/clean.sh
   ```

## 샘플 출력

```
╔════════════════════════════════════════════════════════════════╗
║                    k6 Performance Test Summary                 ║
╚════════════════════════════════════════════════════════════════╝

📊 HTTP Request Metrics:
   • Total Requests: 15,234
   • Avg Duration:   125.42ms
   • P95 Duration:   289.31ms
   • P99 Duration:   512.18ms
   • Failed Rate:    0.02%

📈 Custom Metrics:
   • Token Issue P95:     87.23ms
   • Token Status P95:    45.12ms
   • Reservation P95:     156.78ms
   • Payment P95:         234.56ms

🚦 Thresholds:
   ✅ http_req_duration
   ✅ http_req_failed
   ✅ token_issue_duration
```
