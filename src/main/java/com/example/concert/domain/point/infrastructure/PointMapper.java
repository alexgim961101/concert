package com.example.concert.domain.point.infrastructure;

import com.example.concert.domain.point.entity.Point;

/**
 * Point <-> PointJpaEntity 변환 유틸리티
 */
public class PointMapper {

    public static Point toDomain(PointJpaEntity entity) {
        return Point.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getBalance(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static PointJpaEntity toEntity(Point domain) {
        return new PointJpaEntity(
                domain.getUserId(),
                domain.getBalance());
    }
}
