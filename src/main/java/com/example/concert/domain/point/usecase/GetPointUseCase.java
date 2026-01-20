package com.example.concert.domain.point.usecase;

import com.example.concert.domain.point.entity.Point;
import com.example.concert.domain.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 조회 UseCase
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPointUseCase {
    private final PointRepository pointRepository;

    public Point execute(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }

        return pointRepository.findByUserId(userId)
                .orElseGet(() -> Point.create(userId));
    }
}
