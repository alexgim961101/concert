package com.example.concert.benchmark;

import com.example.concert.config.AbstractIntegrationTest;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.infrastructure.*;
import com.example.concert.domain.concert.service.ConcertService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캐시 성능 벤치마크 테스트
 * 
 * 데이터 규모 (100만 건):
 * - 콘서트: 1,000개
 * - 스케줄: 10,000개 (콘서트당 10개)
 * - 좌석: 1,000,000개 (스케줄당 100개)
 * 
 * 실행 방법:
 * ./gradlew test --tests "*CachePerformanceBenchmarkTest*"
 * -Dorg.gradle.jvmargs="-Xmx4g"
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("캐시 성능 벤치마크 테스트 (100만 건)")
public class CachePerformanceBenchmarkTest extends AbstractIntegrationTest {

    private static final int CONCERT_COUNT = 1000;
    private static final int SCHEDULES_PER_CONCERT = 10;
    private static final int SEATS_PER_SCHEDULE = 100;
    private static final int BATCH_SIZE = 5000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int BENCHMARK_ITERATIONS = 100;

    @Autowired
    private ConcertJpaRepository concertRepository;

    @Autowired
    private ConcertScheduleJpaRepository scheduleRepository;

    @Autowired
    private SeatJpaRepository seatRepository;

    @Autowired
    private ConcertService concertService;

    @Autowired
    private CacheManager cacheManager;

    private List<Long> testConcertIds = new ArrayList<>();
    private List<Long> testScheduleIds = new ArrayList<>();

    // 결과 저장용
    private double avgCacheMissTimeMs;
    private double avgCacheHitTimeMs;

    @BeforeAll
    void setUpOnce() {
        System.out.println("\n========================================");
        System.out.println("캐시 성능 벤치마크 테스트 데이터 준비 시작");
        System.out.println("========================================");
        System.out.println("목표: 콘서트 " + CONCERT_COUNT + "개, 스케줄 " + (CONCERT_COUNT * SCHEDULES_PER_CONCERT) + "개, 좌석 "
                + (CONCERT_COUNT * SCHEDULES_PER_CONCERT * SEATS_PER_SCHEDULE) + "개");

        long startTime = System.currentTimeMillis();

        // 1. 콘서트 생성
        System.out.println("\n[1/3] 콘서트 생성 중...");
        List<ConcertJpaEntity> concerts = new ArrayList<>();
        for (int i = 0; i < CONCERT_COUNT; i++) {
            ConcertJpaEntity concert = new ConcertJpaEntity("Cache Test Concert " + i, "Description " + i);
            concerts.add(concert);
        }
        List<ConcertJpaEntity> savedConcerts = concertRepository.saveAll(concerts);
        testConcertIds = savedConcerts.stream().map(ConcertJpaEntity::getId).toList();
        System.out.println("   -> " + savedConcerts.size() + "개 콘서트 생성 완료");

        // 2. 스케줄 생성
        System.out.println("\n[2/3] 스케줄 생성 중...");
        List<ConcertScheduleJpaEntity> allSchedules = new ArrayList<>();
        for (ConcertJpaEntity concert : savedConcerts) {
            for (int j = 0; j < SCHEDULES_PER_CONCERT; j++) {
                ConcertScheduleJpaEntity schedule = new ConcertScheduleJpaEntity(
                        concert,
                        LocalDateTime.now().plusDays(j + 1),
                        LocalDateTime.now().minusDays(7));
                allSchedules.add(schedule);
            }
        }
        List<ConcertScheduleJpaEntity> savedSchedules = scheduleRepository.saveAll(allSchedules);
        testScheduleIds = savedSchedules.stream().map(ConcertScheduleJpaEntity::getId).toList();
        System.out.println("   -> " + savedSchedules.size() + "개 스케줄 생성 완료");

        // 3. 좌석 생성 (배치 처리)
        System.out.println("\n[3/3] 좌석 생성 중 (" + (savedSchedules.size() * SEATS_PER_SCHEDULE) + "개)...");
        List<SeatJpaEntity> seatBatch = new ArrayList<>();
        int totalSeats = 0;

        for (ConcertScheduleJpaEntity schedule : savedSchedules) {
            for (int k = 1; k <= SEATS_PER_SCHEDULE; k++) {
                SeatJpaEntity seat = new SeatJpaEntity(
                        schedule,
                        k,
                        BigDecimal.valueOf(50000 + ThreadLocalRandom.current().nextInt(100000)));
                seat.setStatus(randomSeatStatus());
                seatBatch.add(seat);

                if (seatBatch.size() >= BATCH_SIZE) {
                    seatRepository.saveAll(seatBatch);
                    totalSeats += seatBatch.size();
                    System.out.print("\r   -> " + totalSeats + "개 좌석 저장됨...");
                    seatBatch.clear();
                }
            }
        }

        if (!seatBatch.isEmpty()) {
            seatRepository.saveAll(seatBatch);
            totalSeats += seatBatch.size();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("\n\n========================================");
        System.out.println("데이터 준비 완료!");
        System.out.println("총 " + totalSeats + "개 좌석 생성");
        System.out.println("소요 시간: " + (elapsed / 1000.0) + "초");
        System.out.println("========================================\n");
    }

    private SeatStatus randomSeatStatus() {
        int rand = ThreadLocalRandom.current().nextInt(100);
        if (rand < 70)
            return SeatStatus.AVAILABLE;
        else if (rand < 90)
            return SeatStatus.TEMP_RESERVED;
        else
            return SeatStatus.RESERVED;
    }

    @Test
    @Order(1)
    @DisplayName("캐시 미적용 (Cache Miss) 성능 측정")
    void benchmark_cacheMiss() {
        System.out.println("\n========================================");
        System.out.println("[테스트 1] 캐시 미적용 (Cache Miss) 성능 측정");
        System.out.println("========================================");

        // 캐시 클리어
        clearAllCaches();

        // 워밍업 (JIT 최적화)
        System.out.println("워밍업 중 (" + WARMUP_ITERATIONS + "회)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Long concertId = testConcertIds.get(i % testConcertIds.size());
            concertService.getSchedulesWithSeats(concertId);
            clearAllCaches();
        }

        // 벤치마크 실행
        System.out.println("벤치마크 시작 (" + BENCHMARK_ITERATIONS + "회, 매번 캐시 클리어)...");
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Long concertId = testConcertIds.get(i % testConcertIds.size());

            // 캐시 클리어 (매번 DB 조회 강제)
            clearAllCaches();

            long start = System.nanoTime();
            var result = concertService.getSchedulesWithSeats(concertId);
            long elapsed = System.nanoTime() - start;

            times.add(elapsed);
            assertThat(result).isNotNull();
        }

        // 통계 계산
        double avgNanos = times.stream().mapToLong(Long::longValue).average().orElse(0);
        double minNanos = times.stream().mapToLong(Long::longValue).min().orElse(0);
        double maxNanos = times.stream().mapToLong(Long::longValue).max().orElse(0);

        avgCacheMissTimeMs = avgNanos / 1_000_000.0;

        System.out.println("\n[결과 - Cache Miss]");
        System.out.println("  평균: " + String.format("%.3f", avgCacheMissTimeMs) + " ms");
        System.out.println("  최소: " + String.format("%.3f", minNanos / 1_000_000.0) + " ms");
        System.out.println("  최대: " + String.format("%.3f", maxNanos / 1_000_000.0) + " ms");
    }

    @Test
    @Order(2)
    @DisplayName("캐시 적용 (Cache Hit) 성능 측정")
    void benchmark_cacheHit() {
        System.out.println("\n========================================");
        System.out.println("[테스트 2] 캐시 적용 (Cache Hit) 성능 측정");
        System.out.println("========================================");

        // 캐시 클리어 후 첫 조회로 캐시 채우기
        clearAllCaches();

        // 캐시 웜업 (테스트할 콘서트들 미리 캐싱)
        System.out.println("캐시 웜업 중...");
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Long concertId = testConcertIds.get(i % testConcertIds.size());
            concertService.getSchedulesWithSeats(concertId);
        }

        // 벤치마크 실행 (캐시 히트)
        System.out.println("벤치마크 시작 (" + BENCHMARK_ITERATIONS + "회, 캐시 히트)...");
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Long concertId = testConcertIds.get(i % testConcertIds.size());

            long start = System.nanoTime();
            var result = concertService.getSchedulesWithSeats(concertId);
            long elapsed = System.nanoTime() - start;

            times.add(elapsed);
            assertThat(result).isNotNull();
        }

        // 통계 계산
        double avgNanos = times.stream().mapToLong(Long::longValue).average().orElse(0);
        double minNanos = times.stream().mapToLong(Long::longValue).min().orElse(0);
        double maxNanos = times.stream().mapToLong(Long::longValue).max().orElse(0);

        avgCacheHitTimeMs = avgNanos / 1_000_000.0;

        System.out.println("\n[결과 - Cache Hit]");
        System.out.println("  평균: " + String.format("%.3f", avgCacheHitTimeMs) + " ms");
        System.out.println("  최소: " + String.format("%.3f", minNanos / 1_000_000.0) + " ms");
        System.out.println("  최대: " + String.format("%.3f", maxNanos / 1_000_000.0) + " ms");
    }

    @Test
    @Order(3)
    @DisplayName("성능 개선율 요약")
    void benchmark_summary() {
        System.out.println("\n========================================");
        System.out.println("캐시 성능 벤치마크 결과 요약");
        System.out.println("========================================");
        System.out.println();
        System.out.println("데이터 규모:");
        System.out.println("  - 콘서트: " + CONCERT_COUNT + "개");
        System.out.println("  - 스케줄: " + (CONCERT_COUNT * SCHEDULES_PER_CONCERT) + "개");
        System.out.println("  - 좌석: " + (CONCERT_COUNT * SCHEDULES_PER_CONCERT * SEATS_PER_SCHEDULE) + "개 (100만건)");
        System.out.println();
        System.out.println("성능 비교:");
        System.out.println("  - Cache Miss (DB 조회): " + String.format("%.3f", avgCacheMissTimeMs) + " ms");
        System.out.println("  - Cache Hit (Redis 조회): " + String.format("%.3f", avgCacheHitTimeMs) + " ms");
        System.out.println();

        if (avgCacheMissTimeMs > 0 && avgCacheHitTimeMs > 0) {
            double improvement = ((avgCacheMissTimeMs - avgCacheHitTimeMs) / avgCacheMissTimeMs) * 100;
            double speedup = avgCacheMissTimeMs / avgCacheHitTimeMs;

            System.out.println("개선율:");
            System.out.println("  - 성능 개선: " + String.format("%.1f", improvement) + "%");
            System.out.println("  - 속도 향상: " + String.format("%.1f", speedup) + "배");
        }
        System.out.println("========================================\n");
    }

    private void clearAllCaches() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(name -> {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }
}
