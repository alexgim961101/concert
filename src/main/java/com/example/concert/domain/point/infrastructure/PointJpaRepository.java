package com.example.concert.domain.point.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<PointJpaEntity, Long> {
    Optional<PointJpaEntity> findByUserId(Long userId);
}
