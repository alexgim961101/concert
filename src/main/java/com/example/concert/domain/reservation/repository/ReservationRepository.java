package com.example.concert.domain.reservation.repository;

import com.example.concert.domain.reservation.entity.Reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 예약 리포지토리 인터페이스 (도메인 계층)
 */
public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    /**
     * 비관적 락을 사용한 예약 조회 (중복 결제 방지)
     */
    Optional<Reservation> findByIdWithLock(Long id);

    List<Reservation> findAllExpired(LocalDateTime now);
}
