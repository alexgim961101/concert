package com.example.concert.domain.reservation.infrastructure;

import com.example.concert.domain.reservation.entity.Reservation;

/**
 * JPA Entity <-> Domain Entity 변환 유틸리티
 */
public class ReservationMapper {

    public static Reservation toDomain(ReservationJpaEntity entity) {
        return Reservation.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getScheduleId(),
                entity.getSeatId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt());
    }

    public static ReservationJpaEntity toEntity(Reservation domain) {
        return new ReservationJpaEntity(
                domain.getUserId(),
                domain.getScheduleId(),
                domain.getSeatId(),
                domain.getStatus(),
                domain.getExpiresAt());
    }
}
