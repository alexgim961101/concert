package com.example.concert.domain.concert.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertJpaRepository extends JpaRepository<ConcertJpaEntity, Long> {
}
