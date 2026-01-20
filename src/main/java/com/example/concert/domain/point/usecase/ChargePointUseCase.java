package com.example.concert.domain.point.usecase;

import com.example.concert.domain.point.entity.Point;
import com.example.concert.domain.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 포인트 충전 UseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargePointUseCase {
    private final PointRepository pointRepository;

    @Transactional
    public ChargeResult execute(Long userId, BigDecimal amount) {
        // 1. 입력값 검증
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        // 2. Mock PG 결제 시도 (항상 성공)
        boolean pgResult = mockPgPayment(userId, amount);
        if (!pgResult) {
            throw new PaymentFailedException("PG 결제에 실패했습니다.");
        }

        // 3. Point 조회 (없으면 신규 생성)
        Point point = pointRepository.findByUserId(userId)
                .orElseGet(() -> Point.create(userId));

        // 4. 포인트 충전 (도메인 로직에서 한도 검증)
        BigDecimal beforeBalance = point.getBalance();
        point.charge(amount);

        // 5. 저장
        Point saved = pointRepository.save(point);

        log.info("Point charged: userId={}, amount={}, before={}, after={}",
                userId, amount, beforeBalance, saved.getBalance());

        return new ChargeResult(saved.getUserId(), saved.getBalance());
    }

    /**
     * Mock PG 결제 (항상 성공 반환)
     */
    private boolean mockPgPayment(Long userId, BigDecimal amount) {
        log.debug("Mock PG payment: userId={}, amount={}", userId, amount);
        return true;
    }

    public record ChargeResult(Long userId, BigDecimal currentBalance) {
    }
}
