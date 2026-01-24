package com.example.concert.domain.point.usecase;

import com.example.concert.common.exception.ConcurrencyConflictException;
import com.example.concert.domain.point.entity.Point;
import com.example.concert.domain.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 포인트 사용 UseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsePointUseCase {
    private final PointRepository pointRepository;

    @Transactional
    public void execute(Long userId, BigDecimal amount) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("포인트 정보가 없습니다. userId=" + userId));

        BigDecimal beforeBalance = point.getBalance();
        point.use(amount);

        // 저장 (낙관적 락으로 동시 사용 충돌 감지)
        try {
            pointRepository.save(point);

            log.info("Point used: userId={}, amount={}, before={}, after={}",
                    userId, amount, beforeBalance, point.getBalance());
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConcurrencyConflictException("포인트 사용 중 충돌이 발생했습니다. 다시 시도해 주세요.");
        }
    }
}
