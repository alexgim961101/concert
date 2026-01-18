package com.example.concert.domain.reservation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {
}
