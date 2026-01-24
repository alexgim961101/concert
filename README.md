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

