package com.example.concert.domain.point.repository;

import com.example.concert.domain.point.entity.Point;

import java.util.Optional;

/**
 * 포인트 리포지토리 인터페이스 (도메인 계층)
 */
public interface PointRepository {
    Optional<Point> findByUserId(Long userId);

    Point save(Point point);
}
