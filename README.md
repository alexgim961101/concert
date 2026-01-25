# 콘서트 예약 서비스 (Concert Reservation Service)

대기열 시스템과 동시성 제어를 통해 안정적인 콘서트 좌석 예약 및 결제를 지원하는 백엔드 서비스입니다.

## 📖 프로젝트 소개
이 프로젝트는 대규모 트래픽 상황에서도 안정적으로 콘서트 좌석을 예약할 수 있는 시스템을 구현하는 것을 목표로 합니다. **대기열 시스템(Queue)** 을 통해 유량 제어를 수행하고, **예약 및 결제** 과정에서의 데이터 일관성을 보장합니다.

### 주요 기능
*   **대기열 시스템**: Redis를 활용하여 대기열 토큰 발급 및 순번 관리 (유량 제어).
*   **콘서트 스케줄링**: 콘서트 날짜 및 회차별 좌석 관리.
*   **좌석 예약**: 동시성 제어를 통한 임시 배정 (좌석 선점) 기능.
*   **포인트 및 결제**: 포인트 충전 및 예약을 위한 포인트 결제 기능.

## 🛠️ 기술 스택 (Tech Stack)
*   **Language**: Java 21
*   **Framework**: Spring Boot 3.5.9
*   **Database**: MySQL 8.0
*   **Cache/Queue**: Redis
*   **Message Broker**: Kafka
*   **Build**: Gradle
*   **Container**: Docker & Docker Compose

## 📐 설계 및 아키텍처
상세한 설계 문서는 `docs/` 디렉토리에서 확인할 수 있습니다.
*   [**시스템 아키텍처**](docs/ARCHITECTURE.md): 시스템 구성 요소 및 서비스 흐름도.
*   [**DB 설계 (ERD)**](docs/ERD.md): 데이터베이스 스키마 및 엔티티 관계.
*   [**API 명세서**](docs/API_SPEC.md): REST API 엔드포인트 및 요청/응답 규격.

## 🚀 실행 방법 (Getting Started)

### 사전 요구사항
*   Docker 및 Docker Compose가 설치되어 있어야 합니다.

### Docker로 전체 실행
단일 명령어로 전체 인프라(MySQL, Redis, Kafka)와 애플리케이션을 실행할 수 있습니다.

```bash
# 빌드 및 실행
docker-compose up -d --build
```

### 접속 정보
*   **API Server**: `http://localhost:8080`
*   **MySQL**: Port `3306`
*   **Redis**: Port `6379`
*   **Kafka**: Port `9092`

## 📅 진행 상황 및 계획
- [x] **인프라 구축**: Docker Compose (MySQL, Redis, Kafka) 구성 완료.
- [x] **시스템 설계**: ERD, 아키텍처, API 명세서 작성 완료.
- [ ] **기능 구현**:
    - [ ] 유저 및 포인트 도메인
    - [ ] 콘서트 및 좌석 도메인
    - [ ] 예약 도메인
    - [ ] 결제 도메인
    - [ ] 대기열 시스템 (Token)
- [ ] **동시성 제어**: 분산 락, 낙관적 락 등을 활용한 동시성 이슈 해결.
- [ ] **테스트**: 단위 테스트 및 통합 테스트 작성.

## 🔧 데이터베이스 인덱스 최적화

쿼리 성능 개선을 위해 다음 인덱스를 추가했습니다.

### 추가된 인덱스

| 테이블 | 인덱스 | 컬럼 | 대상 쿼리 |
|--------|--------|------|----------|
| `queue_tokens` | `idx_queue_tokens_status_id` | `(status, id)` | 대기열 순위 계산 |
| `seats` | `idx_seats_schedule_status` | `(schedule_id, status)` | 스케줄별 좌석 조회 |
| `reservations` | `idx_reservations_status_expires` | `(status, expires_at)` | 만료 예약 배치 처리 |
| `payments` | `idx_payments_user_id` | `(user_id)` | 향후 확장 대비* |
| `payments` | `idx_payments_reservation_id` | `(reservation_id)` | 향후 확장 대비* |

> *`payments` 인덱스는 현재 사용되는 쿼리가 없어 벤치마크에 포함되지 않았습니다. "내 결제 내역 조회", "예약별 결제 정보 조회" 등의 API 추가 시 활용됩니다.

### 인덱스 추가 이유

1. **`queue_tokens`**: `countByStatusAndIdLessThan` 쿼리에서 Full Table Scan 방지
2. **`seats`**: FK 인덱스만으로는 status 필터링 시 추가 스캔 필요, 복합 인덱스로 Covering Index 효과
3. **`reservations`**: 만료 예약 배치 처리 시 효율적 조회
4. **`payments`**: 향후 결제 내역 조회 API 확장 대비

### 성능 측정 결과

**테스트 데이터 규모**: 좌석 1,000,000개, 대기열 토큰 1,000,000개, 예약 500,000개

| Repository | 메서드 | Baseline | Indexed | 변화 |
|------------|--------|----------|---------|------|
| `SeatJpaRepository` | `countByScheduleIdAndStatus` | 150.9 ms | 0.29 ms | **-99.8%** 🔥 |
| `SeatJpaRepository` | `findByScheduleIdAndStatusIn` | 194.7 ms | 40.2 ms | **-79%** |
| `ReservationJpaRepository` | `findAllByStatusAndExpiresAtBefore` | 75.6 ms | 16.3 ms | **-78%** |

> **인덱스 효과**: 100만 건 규모에서 핵심 쿼리가 **5~500배** 빨라졌습니다.

#### 왜 `countByScheduleIdAndStatus`가 99.8% 개선되었나?

**Covering Index** 효과 때문입니다:

| 쿼리 타입 | 반환 | 테이블 접근 | 성능 개선 |
|-----------|------|-------------|-----------|
| `COUNT` 쿼리 | 숫자 1개 | ❌ 불필요 | **극적** (99.8%) |
| `SELECT *` 쿼리 | 객체 리스트 | ✅ 필요 | 높음 (78~79%) |

- `COUNT(*)` 쿼리는 인덱스 `(schedule_id, status)`만으로 결과를 반환 가능
- 인덱스에 필요한 모든 데이터가 있어 **테이블 I/O 완전 제거**
- 반면 `SELECT *` 쿼리는 인덱스 Lookup 후 **테이블에서 실제 데이터를 읽어야 함**

### 벤치마크 실행 방법

```bash
./gradlew test --tests "*PerformanceBenchmarkTest*"
```

---

## 🔒 동시성 제어

### 동시성 위험 지점 및 해결 방식

| 위험 지점 | UseCase | 문제 시나리오 | 해결 방식 |
|-----------|---------|---------------|-----------|
| **좌석 예약** | `ReserveSeatUseCase` | 동시에 같은 좌석 예약 → 중복 예약 | **Redis 분산 락 (Redisson)** |
| **포인트 충전** | `ChargePointUseCase` | 동시 충전 → 일부 누락 | DB 낙관적 락 (`@Version`) |
| **포인트 사용** | `UsePointUseCase` | 동시 사용 → 잔액 불일치 | DB 낙관적 락 (`@Version`) |
| **결제 처리** | `ProcessPaymentUseCase` | 중복 결제 | 예약 비관적 락 |

---

### 동시성 제어 방법 비교

| 방식 | 설명 | 장점 | 단점 | 적합한 상황 |
|------|------|------|------|-------------|
| **DB 비관적 락** | `SELECT ... FOR UPDATE`로 행 잠금 | 충돌 완전 방지, 즉시 대기 | 성능 저하, 데드락 가능 | 충돌 빈도 높음, 데이터 정합성 필수 |
| **DB 낙관적 락** | `@Version` 필드로 변경 감지 | 성능 우수, 데드락 없음 | 충돌 시 재시도 필요 | 충돌 빈도 낮음, 읽기 위주 |
| **Redis 분산 락** | Redisson/Lettuce로 분산 잠금 | DB 부하 분산, 확장성 | Redis 의존성, TTL 관리 필요 | 외부 API 제한, 분산 캐시 동기화 |
| **Application 락** | `synchronized` / `ReentrantLock` | 구현 간단 | 단일 인스턴스에서만 유효 | 단일 서버, 인메모리 작업 |
| **DB Unique 제약** | `UNIQUE INDEX`로 중복 방지 | 가장 간단, DB 레벨 보장 | 사후 방어만 가능 | 중복 삽입 방지 (예: 1인 1예약) |

---

### 선택 이유

#### 좌석 예약: **Redis 분산 락** 선택

```
❌ 낙관적 락: 인기 좌석은 충돌 빈도가 매우 높아 재시도가 반복됨
❌ DB 비관적 락: 대기 시 DB 커넥션을 점유하여 커넥션 풀 고갈 위험
✅ Redis 분산 락: DB 부하 분산, 커넥션 풀 보호, 빠른 실패 처리
```

**선택 근거:**
- 콘서트 티켓팅은 특성상 **동일 자원(좌석)에 대한 경쟁이 매우 높음**
- DB 비관적 락 사용 시 **수백 개의 트랜잭션이 DB 커넥션을 점유하며 대기** → 커넥션 풀 고갈 위험
- Redis 분산 락으로 **락 획득한 요청만 DB에 접근** → DB 부하 감소
- `@DistributedLock` 커스텀 어노테이션 + AOP로 **비즈니스 로직과 락 로직 분리`

#### 포인트 충전/사용: **낙관적 락** 선택

```
❌ 비관적 락: 사용자별 포인트는 충돌 확률이 낮아 락 오버헤드 발생
❌ Redis 분산 락: 과도한 인프라 복잡도 추가
✅ 낙관적 락: 충돌 발생 시에만 예외 처리, 평소에는 락 오버헤드 없음
```

**선택 근거:**
- 포인트는 **사용자별로 독립적** → 같은 사용자가 동시에 충전/사용하는 경우만 충돌
- 일반적으로 충돌 확률이 낮아 **대부분의 요청이 락 오버헤드 없이 성공**
- 기존 `@Version` 필드가 이미 존재하여 별도 인프라 필요 없음
- 충돌 시 `ConcurrencyConflictException`으로 클라이언트에 재시도 유도 (HTTP 409)

#### 왜 Redis 분산 락을 선택했나?

| 기준 | DB 비관적 락 (이전) | Redis 분산 락 (현재) |
|------|---------------------|----------------------|
| DB 부하 | 락 대기 시 커넥션 점유 | DB 접근 최소화 |
| 처리량 | 낮음 | 높음 (메모리 기반) |
| 확장성 | DB 락 처리량 한계 | 수평 확장 용이 |
| 복잡도 | 간단 (`@Lock` 사용) | AOP 기반 구현 필요 |

**변경 이유:**
- 대규모 트래픽 시 **DB 커넥션 풀 고갈 위험 방지**
- 락 획득 실패 시 **빠른 실패 처리 (Fail Fast)**로 사용자 경험 개선
- Redis가 이미 대기열 시스템에 사용 중이므로 **추가 인프라 비용 없음**

---

### 다중 인스턴스 환경

**DB 락은 다중 인스턴스(Scale-out)에서 정상 작동합니다:**

```
Instance A: SELECT ... FOR UPDATE (seat_id=1) → 락 획득 ✅
Instance B: SELECT ... FOR UPDATE (seat_id=1) → 대기 중... ⏳
Instance A: COMMIT → 락 해제
Instance B: 락 획득 → 좌석 이미 예약됨 → 실패
```

DB 락은 **데이터베이스 레벨**에서 관리되므로, 어떤 인스턴스에서 요청이 들어와도 일관된 동시성 제어가 보장됩니다.

---

### 동시성 테스트 실행

```bash
./gradlew test --tests "*ConcurrencyIntegrationTest*"
```

---

## 🚀 캐싱 전략 (Caching Strategy)

### 왜 캐싱이 필요한가?

콘서트 예약 서비스는 **티켓 오픈 시점에 수천 명이 동시 조회**하는 특성이 있습니다. 스케줄 조회, 가용 좌석 수 계산 등의 쿼리가 매번 DB를 때리면 과부하가 발생합니다.

### 왜 Redis인가?

| 기준 | Redis | Local Cache (Caffeine 등) |
|------|-------|---------------------------|
| 다중 인스턴스 | ✅ 공유 캐시 | ❌ 인스턴스별 별도 |
| 데이터 일관성 | ✅ 중앙 관리 | ❌ 동기화 필요 |
| 기존 인프라 | ✅ 이미 사용 중 (대기열) | - |
| 성능 | O(1) 조회 | O(1) 조회 |

→ **이미 Redis 인프라가 있고, Scale-out 환경에서 캐시 일관성이 중요**하므로 Redis 선택.

### 적용 방식

| 대상 | TTL | 전략 |
|------|-----|------|
| **콘서트 스케줄 + 가용 좌석 수** | 5분 ± 30초 | 읽기 시 캐싱, 재조회 시 캐시 히트 |
| **좌석 목록** | 30초 ± 5초 | 예약 성공 시 Write-Through로 즉시 갱신 |

**핵심 설계:**
- **TTL Jitter**: 캐시 만료 시간에 랜덤 값 추가 → Cache Avalanche 방지
- **Cache Stampede 방지**: `sync = true` 옵션으로 동일 키에 대해 한 스레드만 DB 조회
- **Write-Through**: 좌석 예약 시 캐시 삭제 대신 갱신 → Eviction Stampede 방지
- **캐시 웜업**: 애플리케이션 시작 시 7일 내 임박 콘서트 미리 캐싱

### 성능 개선 (실측)

100만건 데이터 기준 벤치마크 결과:

| 시나리오 | 응답 시간 | 비고 |
|----------|----------|------|
| **Cache Miss (DB 조회)** | 6.48 ms | 스케줄 조회 + 좌석 COUNT 쿼리 |
| **Cache Hit (Redis 조회)** | 0.33 ms | 직렬화된 결과 반환 |
| **성능 개선율** | **94.9%** | |
| **속도 향상** | **19.6배** | |

### 벤치마크 실행

```bash
./gradlew test --tests "*CachePerformanceBenchmarkTest*" -Dorg.gradle.jvmargs="-Xmx4g"
```

> Docker + Redis가 필요합니다. 실제 결과는 환경에 따라 달라질 수 있습니다.


