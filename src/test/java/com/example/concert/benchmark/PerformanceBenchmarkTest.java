package com.example.concert.benchmark;

import com.example.concert.config.AbstractIntegrationTest;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.infrastructure.ConcertJpaEntity;
import com.example.concert.domain.concert.infrastructure.ConcertJpaRepository;
import com.example.concert.domain.concert.infrastructure.ConcertScheduleJpaEntity;
import com.example.concert.domain.concert.infrastructure.ConcertScheduleJpaRepository;
import com.example.concert.domain.concert.infrastructure.SeatJpaEntity;
import com.example.concert.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert.domain.point.infrastructure.PointJpaEntity;
import com.example.concert.domain.point.infrastructure.PointJpaRepository;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.infrastructure.QueueTokenJpaEntity;
import com.example.concert.domain.queue.infrastructure.QueueTokenJpaRepository;
import com.example.concert.domain.reservation.entity.ReservationStatus;
import com.example.concert.domain.reservation.infrastructure.ReservationJpaEntity;
import com.example.concert.domain.reservation.infrastructure.ReservationJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인덱스 추가 전후 성능 비교를 위한 벤치마크 테스트.
 * 
 * 데이터 규모 (100만 건):
 * - 콘서트: 100개
 * - 스케줄: 10,000개 (콘서트당 100개)
 * - 좌석: 1,000,000개 (스케줄당 100개)
 * - 대기열 토큰: 1,000,000개
 * - 예약: 500,000개
 * - 포인트: 500,000개
 * 
 * 실행 방법:
 * ./gradlew test --tests "*PerformanceBenchmarkTest*"
 * -Dorg.gradle.jvmargs="-Xmx4g"
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("인덱스 성능 벤치마크 테스트 (100만 건)")
class PerformanceBenchmarkTest extends AbstractIntegrationTest {

    @Autowired
    private ConcertJpaRepository concertJpaRepository;

    @Autowired
    private ConcertScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private SeatJpaRepository seatJpaRepository;

    @Autowired
    private QueueTokenJpaRepository queueTokenJpaRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    // 100만 건 규모
    private static final int CONCERT_COUNT = 100;
    private static final int SCHEDULES_PER_CONCERT = 100;
    private static final int SEATS_PER_SCHEDULE = 100;
    private static final int TOKEN_COUNT = 1_000_000;
    private static final int RESERVATION_COUNT = 500_000;
    private static final int POINT_COUNT = 500_000;
    private static final int BENCHMARK_ITERATIONS = 100;
    private static final int BATCH_SIZE = 10_000;

    private List<Long> scheduleIds = new ArrayList<>();
    private List<String> tokenValues = new ArrayList<>();
    private List<Long> tokenIds = new ArrayList<>();
    private List<Long> userIds = new ArrayList<>();

    @BeforeAll
    void setUpOnce() {
        System.out.println("=== 테스트 데이터 생성 시작 (100만 건 규모) ===");
        long startTime = System.currentTimeMillis();

        // 1. 콘서트 생성
        List<ConcertJpaEntity> concerts = new ArrayList<>();
        for (int i = 0; i < CONCERT_COUNT; i++) {
            ConcertJpaEntity concert = new ConcertJpaEntity("Concert " + i, "Description " + i);
            concerts.add(concertJpaRepository.save(concert));
        }
        System.out.println("콘서트 " + CONCERT_COUNT + "개 생성 완료");

        // 2. 스케줄 생성 (Batch)
        List<ConcertScheduleJpaEntity> scheduleBatch = new ArrayList<>();
        for (ConcertJpaEntity concert : concerts) {
            for (int i = 0; i < SCHEDULES_PER_CONCERT; i++) {
                ConcertScheduleJpaEntity schedule = new ConcertScheduleJpaEntity(
                        concert,
                        LocalDateTime.now().plusDays((i % 365) + 1),
                        LocalDateTime.now().minusDays(7));
                scheduleBatch.add(schedule);
            }
        }
        List<ConcertScheduleJpaEntity> savedSchedules = scheduleJpaRepository.saveAll(scheduleBatch);
        scheduleIds = savedSchedules.stream().map(ConcertScheduleJpaEntity::getId).toList();
        System.out.println("스케줄 " + savedSchedules.size() + "개 생성 완료");

        // 3. 좌석 생성 (Batch Insert)
        int seatCount = 0;
        List<SeatJpaEntity> seatBatch = new ArrayList<>();
        for (ConcertScheduleJpaEntity schedule : savedSchedules) {
            for (int seatNum = 1; seatNum <= SEATS_PER_SCHEDULE; seatNum++) {
                SeatStatus status = randomSeatStatus();
                SeatJpaEntity seat = new SeatJpaEntity(schedule, seatNum, BigDecimal.valueOf(50000 + seatNum * 1000));
                seat.setStatus(status);
                seatBatch.add(seat);
                seatCount++;

                if (seatBatch.size() >= BATCH_SIZE) {
                    seatJpaRepository.saveAll(seatBatch);
                    seatBatch.clear();
                    if (seatCount % 100_000 == 0) {
                        System.out.println("좌석 " + seatCount + "개 생성 중...");
                    }
                }
            }
        }
        if (!seatBatch.isEmpty()) {
            seatJpaRepository.saveAll(seatBatch);
        }
        System.out.println("좌석 " + seatCount + "개 생성 완료");

        // 4. 대기열 토큰 생성 (Batch Insert)
        List<QueueTokenJpaEntity> tokenBatch = new ArrayList<>();
        for (int i = 0; i < TOKEN_COUNT; i++) {
            TokenStatus status = randomTokenStatus();
            LocalDateTime expiresAt = status == TokenStatus.EXPIRED
                    ? LocalDateTime.now().minusHours(1)
                    : LocalDateTime.now().plusMinutes(30);

            String token = UUID.randomUUID().toString();
            QueueTokenJpaEntity entity = new QueueTokenJpaEntity(
                    (long) i + 1,
                    (long) (i % CONCERT_COUNT) + 1,
                    token,
                    status,
                    expiresAt);
            tokenBatch.add(entity);
            tokenValues.add(token);

            if (tokenBatch.size() >= BATCH_SIZE) {
                List<QueueTokenJpaEntity> saved = queueTokenJpaRepository.saveAll(tokenBatch);
                saved.forEach(e -> tokenIds.add(e.getId()));
                tokenBatch.clear();
                if ((i + 1) % 100_000 == 0) {
                    System.out.println("대기열 토큰 " + (i + 1) + "개 생성 중...");
                }
            }
        }
        if (!tokenBatch.isEmpty()) {
            List<QueueTokenJpaEntity> saved = queueTokenJpaRepository.saveAll(tokenBatch);
            saved.forEach(e -> tokenIds.add(e.getId()));
        }
        System.out.println("대기열 토큰 " + TOKEN_COUNT + "개 생성 완료");

        // 5. 예약 생성 (Batch Insert)
        List<ReservationJpaEntity> reservationBatch = new ArrayList<>();
        for (int i = 0; i < RESERVATION_COUNT; i++) {
            ReservationStatus status = randomReservationStatus();
            LocalDateTime expiresAt = status == ReservationStatus.PENDING
                    ? (ThreadLocalRandom.current().nextBoolean()
                            ? LocalDateTime.now().minusMinutes(10)
                            : LocalDateTime.now().plusMinutes(5))
                    : LocalDateTime.now().plusMinutes(5);

            ReservationJpaEntity reservation = new ReservationJpaEntity(
                    (long) i + 1,
                    scheduleIds.get(i % scheduleIds.size()),
                    (long) i + 1,
                    status,
                    expiresAt);
            reservationBatch.add(reservation);

            if (reservationBatch.size() >= BATCH_SIZE) {
                reservationJpaRepository.saveAll(reservationBatch);
                reservationBatch.clear();
                if ((i + 1) % 100_000 == 0) {
                    System.out.println("예약 " + (i + 1) + "개 생성 중...");
                }
            }
        }
        if (!reservationBatch.isEmpty()) {
            reservationJpaRepository.saveAll(reservationBatch);
        }
        System.out.println("예약 " + RESERVATION_COUNT + "개 생성 완료");

        // 6. 포인트 생성 (Batch Insert)
        List<PointJpaEntity> pointBatch = new ArrayList<>();
        for (int i = 0; i < POINT_COUNT; i++) {
            PointJpaEntity point = new PointJpaEntity(
                    (long) i + 1,
                    BigDecimal.valueOf(100000 + ThreadLocalRandom.current().nextInt(900000)));
            pointBatch.add(point);
            userIds.add((long) i + 1);

            if (pointBatch.size() >= BATCH_SIZE) {
                pointJpaRepository.saveAll(pointBatch);
                pointBatch.clear();
                if ((i + 1) % 100_000 == 0) {
                    System.out.println("포인트 " + (i + 1) + "개 생성 중...");
                }
            }
        }
        if (!pointBatch.isEmpty()) {
            pointJpaRepository.saveAll(pointBatch);
        }
        System.out.println("포인트 " + POINT_COUNT + "개 생성 완료");

        long endTime = System.currentTimeMillis();
        long elapsedMinutes = (endTime - startTime) / 60000;
        long elapsedSeconds = ((endTime - startTime) % 60000) / 1000;
        System.out.printf("=== 데이터 생성 완료: %d분 %d초 ===%n%n", elapsedMinutes, elapsedSeconds);
    }

    private SeatStatus randomSeatStatus() {
        double rand = ThreadLocalRandom.current().nextDouble();
        if (rand < 0.7)
            return SeatStatus.AVAILABLE;
        if (rand < 0.9)
            return SeatStatus.RESERVED;
        return SeatStatus.TEMP_RESERVED;
    }

    private TokenStatus randomTokenStatus() {
        double rand = ThreadLocalRandom.current().nextDouble();
        if (rand < 0.6)
            return TokenStatus.WAITING;
        if (rand < 0.9)
            return TokenStatus.ACTIVE;
        return TokenStatus.EXPIRED;
    }

    private ReservationStatus randomReservationStatus() {
        double rand = ThreadLocalRandom.current().nextDouble();
        if (rand < 0.4)
            return ReservationStatus.PENDING;
        if (rand < 0.8)
            return ReservationStatus.CONFIRMED;
        return ReservationStatus.CANCELLED;
    }

    @Test
    @Order(1)
    @DisplayName("1. QueueToken - findByToken 성능 측정")
    void benchmark_findByToken() {
        List<String> sampleTokens = tokenValues.subList(0, Math.min(BENCHMARK_ITERATIONS, tokenValues.size()));

        long totalTime = 0;
        for (String token : sampleTokens) {
            long start = System.nanoTime();
            queueTokenJpaRepository.findByToken(token);
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        double avgMs = (totalTime / (double) sampleTokens.size()) / 1_000_000.0;
        System.out.printf("[findByToken] 평균 실행 시간: %.3f ms (총 %d회)%n", avgMs, sampleTokens.size());

        assertThat(avgMs).as("findByToken should complete in reasonable time").isLessThan(1000);
    }

    @Test
    @Order(2)
    @DisplayName("2. QueueToken - countByStatusAndIdLessThan 성능 측정 (인덱스 핵심 대상)")
    void benchmark_countByStatusAndIdLessThan() {
        List<Long> sampleIds = tokenIds.stream()
                .filter(id -> id > TOKEN_COUNT / 4 && id < TOKEN_COUNT * 3 / 4)
                .limit(BENCHMARK_ITERATIONS)
                .toList();

        long totalTime = 0;
        for (Long id : sampleIds) {
            long start = System.nanoTime();
            queueTokenJpaRepository.countByStatusAndIdLessThan(TokenStatus.WAITING, id);
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        double avgMs = (totalTime / (double) sampleIds.size()) / 1_000_000.0;
        System.out.printf("[countByStatusAndIdLessThan] 평균 실행 시간: %.3f ms (총 %d회)%n", avgMs, sampleIds.size());

        assertThat(avgMs).as("countByStatusAndIdLessThan should complete in reasonable time").isLessThan(5000);
    }

    @Test
    @Order(3)
    @DisplayName("3. Seat - findByScheduleIdAndStatusIn 성능 측정 (인덱스 핵심 대상)")
    void benchmark_findByScheduleIdAndStatusIn() {
        List<Long> sampleScheduleIds = scheduleIds.subList(0, Math.min(BENCHMARK_ITERATIONS, scheduleIds.size()));

        long totalTime = 0;
        for (Long scheduleId : sampleScheduleIds) {
            long start = System.nanoTime();
            seatJpaRepository.findByScheduleIdAndStatusIn(scheduleId, List.of(SeatStatus.AVAILABLE));
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        double avgMs = (totalTime / (double) sampleScheduleIds.size()) / 1_000_000.0;
        System.out.printf("[findByScheduleIdAndStatusIn] 평균 실행 시간: %.3f ms (총 %d회)%n", avgMs, sampleScheduleIds.size());

        assertThat(avgMs).as("findByScheduleIdAndStatusIn should complete in reasonable time").isLessThan(1000);
    }

    @Test
    @Order(4)
    @DisplayName("4. Seat - countByScheduleIdAndStatus 성능 측정")
    void benchmark_countByScheduleIdAndStatus() {
        List<Long> sampleScheduleIds = scheduleIds.subList(0, Math.min(BENCHMARK_ITERATIONS, scheduleIds.size()));

        long totalTime = 0;
        for (Long scheduleId : sampleScheduleIds) {
            long start = System.nanoTime();
            seatJpaRepository.countByScheduleIdAndStatus(scheduleId, SeatStatus.AVAILABLE);
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        double avgMs = (totalTime / (double) sampleScheduleIds.size()) / 1_000_000.0;
        System.out.printf("[countByScheduleIdAndStatus] 평균 실행 시간: %.3f ms (총 %d회)%n", avgMs, sampleScheduleIds.size());

        assertThat(avgMs).as("countByScheduleIdAndStatus should complete in reasonable time").isLessThan(1000);
    }

    @Test
    @Order(5)
    @DisplayName("5. Reservation - findAllByStatusAndExpiresAtBefore 성능 측정 (인덱스 핵심 대상)")
    void benchmark_findAllByStatusAndExpiresAtBefore() {
        long totalTime = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            reservationJpaRepository.findAllByStatusAndExpiresAtBefore(
                    ReservationStatus.PENDING,
                    LocalDateTime.now());
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        double avgMs = (totalTime / (double) BENCHMARK_ITERATIONS) / 1_000_000.0;
        System.out.printf("[findAllByStatusAndExpiresAtBefore] 평균 실행 시간: %.3f ms (총 %d회)%n", avgMs,
                BENCHMARK_ITERATIONS);

        assertThat(avgMs).as("findAllByStatusAndExpiresAtBefore should complete in reasonable time").isLessThan(5000);
    }

    @Test
    @Order(6)
    @DisplayName("6. Point - findByUserId 성능 측정")
    void benchmark_findByUserId() {
        List<Long> sampleUserIds = userIds.subList(0, Math.min(BENCHMARK_ITERATIONS, userIds.size()));

        long totalTime = 0;
        for (Long userId : sampleUserIds) {
            long start = System.nanoTime();
            pointJpaRepository.findByUserId(userId);
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        double avgMs = (totalTime / (double) sampleUserIds.size()) / 1_000_000.0;
        System.out.printf("[findByUserId] 평균 실행 시간: %.3f ms (총 %d회)%n", avgMs, sampleUserIds.size());

        assertThat(avgMs).as("findByUserId should complete in reasonable time").isLessThan(1000);
    }

    @Test
    @Order(7)
    @DisplayName("7. 전체 벤치마크 결과 요약")
    void benchmark_summary() {
        System.out.println("\n========================================");
        System.out.println("   벤치마크 결과 요약 (100만 건 규모)");
        System.out.println("========================================");
        System.out.println("데이터 규모:");
        System.out.printf("  - 콘서트: %,d개%n", CONCERT_COUNT);
        System.out.printf("  - 스케줄: %,d개%n", CONCERT_COUNT * SCHEDULES_PER_CONCERT);
        System.out.printf("  - 좌석: %,d개%n", CONCERT_COUNT * SCHEDULES_PER_CONCERT * SEATS_PER_SCHEDULE);
        System.out.printf("  - 대기열 토큰: %,d개%n", TOKEN_COUNT);
        System.out.printf("  - 예약: %,d개%n", RESERVATION_COUNT);
        System.out.printf("  - 포인트: %,d개%n", POINT_COUNT);
        System.out.println("========================================");
        System.out.println("각 테스트 결과는 위 로그를 참조하세요.");
        System.out.println("인덱스 적용 후 다시 실행하여 성능을 비교하세요.");
        System.out.println("========================================\n");
    }
}
