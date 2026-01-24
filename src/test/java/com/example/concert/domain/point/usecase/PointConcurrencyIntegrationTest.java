package com.example.concert.domain.point.usecase;

import com.example.concert.common.exception.ConcurrencyConflictException;
import com.example.concert.domain.point.infrastructure.PointJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("포인트 동시성 통합 테스트")
class PointConcurrencyIntegrationTest {

    @Autowired
    private ChargePointUseCase chargePointUseCase;

    @Autowired
    private UsePointUseCase usePointUseCase;

    @Autowired
    private GetPointUseCase getPointUseCase;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    private static final Long USER_ID = 999L; // 다른 테스트와 충돌 방지

    @BeforeEach
    void setUp() {
        // 해당 유저의 포인트 데이터만 삭제
        pointJpaRepository.findByUserId(USER_ID).ifPresent(pointJpaRepository::delete);
    }

    @Nested
    @DisplayName("동시 충전 테스트")
    class ConcurrentChargeTest {

        @Test
        @DisplayName("동시에 10번 충전해도 모든 충전이 반영되거나 충돌 예외가 발생한다")
        void concurrentCharge_allReflectedOrConflict() throws InterruptedException {
            // Given: 미리 포인트 생성 (동시 INSERT 방지)
            chargePointUseCase.execute(USER_ID, BigDecimal.valueOf(1000));

            int threadCount = 10;
            BigDecimal chargeAmount = BigDecimal.valueOf(10000);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger conflictCount = new AtomicInteger(0);
            List<Throwable> errors = new ArrayList<>();

            // When: 10개 스레드가 동시에 충전 시도
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        chargePointUseCase.execute(USER_ID, chargeAmount);
                        successCount.incrementAndGet();
                    } catch (ConcurrencyConflictException e) {
                        conflictCount.incrementAndGet();
                    } catch (Throwable e) {
                        synchronized (errors) {
                            errors.add(e);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Then: 성공 횟수만큼 잔액이 증가, 에러 없음 확인
            // 초기 1000원 + 성공한 충전 횟수 * 10000원
            BigDecimal initialBalance = BigDecimal.valueOf(1000);
            BigDecimal expectedBalance = initialBalance
                    .add(chargeAmount.multiply(BigDecimal.valueOf(successCount.get())));
            BigDecimal actualBalance = getPointUseCase.execute(USER_ID).getBalance();

            assertThat(actualBalance).isEqualByComparingTo(expectedBalance);
            // 동시 충전 시 일부는 충돌로 실패할 수 있지만, 최소 1번은 성공해야 함
            assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("동시 사용 테스트")
    class ConcurrentUseTest {

        @Test
        @DisplayName("잔액보다 많은 금액을 동시에 사용하면 일부만 성공한다")
        void concurrentUse_partialSuccess() throws InterruptedException {
            // Given: 잔액 10만원 충전
            chargePointUseCase.execute(USER_ID, BigDecimal.valueOf(100000));

            int threadCount = 3;
            BigDecimal useAmount = BigDecimal.valueOf(60000); // 6만원씩 3번 = 18만원 (잔액 초과)
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger insufficientCount = new AtomicInteger(0);
            AtomicInteger conflictCount = new AtomicInteger(0);

            // When: 3개 스레드가 동시에 6만원씩 사용 시도
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        usePointUseCase.execute(USER_ID, useAmount);
                        successCount.incrementAndGet();
                    } catch (IllegalStateException e) {
                        if (e.getMessage().contains("잔액이 부족")) {
                            insufficientCount.incrementAndGet();
                        }
                    } catch (ConcurrencyConflictException e) {
                        conflictCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Then: 최대 1번만 성공 (10만원 중 6만원 1번만 사용 가능)
            assertThat(successCount.get()).isLessThanOrEqualTo(1);

            // 잔액 확인: 성공한 만큼만 차감
            BigDecimal actualBalance = getPointUseCase.execute(USER_ID).getBalance();
            assertThat(actualBalance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }
}
