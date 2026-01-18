package com.example.concert.domain.reservation.repository;

import com.example.concert.domain.reservation.entity.Reservation;

import java.util.Optional;

/**
 * 예약 리포지토리 인터페이스 (도메인 계층)
 */
public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);
}
