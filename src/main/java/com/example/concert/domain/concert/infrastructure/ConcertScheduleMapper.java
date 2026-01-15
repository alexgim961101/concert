package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.ConcertSchedule;

public class ConcertScheduleMapper {
    public static ConcertSchedule toDomain(ConcertScheduleJpaEntity entity) {
        if (entity == null)
            return null;
        return new ConcertSchedule(
                entity.getId(),
                entity.getConcertId(),
                entity.getConcertDate(),
                entity.getReservationStartAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
