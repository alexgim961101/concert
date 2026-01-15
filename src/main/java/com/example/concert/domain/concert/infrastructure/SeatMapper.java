package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.Seat;

public class SeatMapper {

    public static Seat toDomain(SeatJpaEntity entity) {
        return new Seat(
                entity.getId(),
                entity.getScheduleId(),
                entity.getSeatNumber(),
                entity.getPrice(),
                entity.getStatus(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
