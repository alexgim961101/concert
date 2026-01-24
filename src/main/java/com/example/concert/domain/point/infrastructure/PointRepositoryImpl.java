package com.example.concert.domain.point.infrastructure;

import com.example.concert.domain.point.entity.Point;
import com.example.concert.domain.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository {
    private final PointJpaRepository jpaRepository;

    @Override
    public Optional<Point> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
                .map(PointMapper::toDomain);
    }

    @Override
    public Point save(Point point) {
        PointJpaEntity entity;
        if (point.getId() != null) {
            // 기존 포인트 업데이트
            entity = jpaRepository.findById(point.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Point not found: " + point.getId()));
            entity.setBalance(point.getBalance());
        } else {
            // 신규 생성
            entity = PointMapper.toEntity(point);
        }
        // saveAndFlush로 즉시 반영하여 낙관적 락 예외를 즉시 감지
        PointJpaEntity saved = jpaRepository.saveAndFlush(entity);
        return PointMapper.toDomain(saved);
    }
}
