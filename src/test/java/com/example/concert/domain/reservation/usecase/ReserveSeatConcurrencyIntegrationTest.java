package com.example.concert.domain.reservation.usecase;

import com.example.concert.config.AbstractIntegrationTest;
import com.example.concert.domain.concert.infrastructure.ConcertJpaEntity;
import com.example.concert.domain.concert.infrastructure.ConcertJpaRepository;
import com.example.concert.domain.concert.infrastructure.ConcertScheduleJpaEntity;
import com.example.concert.domain.concert.infrastructure.ConcertScheduleJpaRepository;
import com.example.concert.domain.concert.infrastructure.SeatJpaEntity;
import com.example.concert.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.infrastructure.RedisQueueTokenRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("좌석 예약 동시성 통합 테스트")
class ReserveSeatConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;

    @Autowired
    private ConcertJpaRepository concertJpaRepository;

    @Autowired
    private ConcertScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private SeatJpaRepository seatJpaRepository;

    @Autowired
    private RedisQueueTokenRepositoryImpl queueTokenRepository;

    private Long scheduleId;
    private Long seatId;
    private List<String> tokens = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리 (DB만)
        seatJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();
        concertJpaRepository.deleteAll();

        // Concert 생성
        ConcertJpaEntity concert = concertJpaRepository.save(
                new ConcertJpaEntity("동시성 테스트 콘서트", "테스트 설명"));

        // Schedule 생성
        ConcertScheduleJpaEntity schedule = scheduleJpaRepository.save(
                new ConcertScheduleJpaEntity(
                        concert,
                        LocalDateTime.now().plusDays(30),
                        LocalDateTime.now().minusDays(1)));
        scheduleId = schedule.getId();

        // Seat 생성 (1개만)
        SeatJpaEntity seat = seatJpaRepository.save(
                new SeatJpaEntity(schedule, 1, BigDecimal.valueOf(10000)));
        seatId = seat.getId();

        // Redis 기반 토큰 10개 생성
        tokens.clear();
        for (int i = 1; i <= 10; i++) {
            QueueToken token = new QueueToken((long) i, concert.getId(), LocalDateTime.now().plusMinutes(30));
            token.activate();
            QueueToken saved = queueTokenRepository.save(token);
            tokens.add(saved.getToken());
        }
    }

    @Test
    @DisplayName("10명이 동시에 같은 좌석을 예약하면 1명만 성공한다")
    void concurrentSeatReservation_onlyOneSucceeds() throws InterruptedException {
        // Given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        // When: 10개 스레드가 동시에 같은 좌석 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    reserveSeatUseCase.execute(
                            tokens.get(index),
                            (long) (index + 1),
                            scheduleId,
                            seatId);
                    successCount.incrementAndGet();
                } catch (SeatNotAvailableException e) {
                    failCount.incrementAndGet();
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then: 1명만 성공, 9명은 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);
        assertThat(errors).isEmpty();
    }
}
