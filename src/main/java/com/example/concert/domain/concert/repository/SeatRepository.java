package com.example.concert.domain.concert.repository;

import com.example.concert.domain.concert.entity.SeatStatus;

public interface SeatRepository {
    int countByScheduleIdAndStatus(Long scheduleId, SeatStatus status);
}
