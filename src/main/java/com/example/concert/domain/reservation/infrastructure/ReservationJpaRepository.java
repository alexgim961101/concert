package com.example.concert.domain.reservation.infrastructure;

import com.example.concert.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

    @Query("SELECT r FROM ReservationJpaEntity r WHERE r.status = :status AND r.expiresAt < :now")
    List<ReservationJpaEntity> findAllByStatusAndExpiresAtBefore(
            @Param("status") ReservationStatus status,
            @Param("now") LocalDateTime now);
}
