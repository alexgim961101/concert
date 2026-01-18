package com.example.concert.domain.concert.repository;

import com.example.concert.domain.concert.entity.Seat;
import com.example.concert.domain.concert.entity.SeatStatus;

import java.util.List;
import java.util.Optional;

public interface SeatRepository {
    int countByScheduleIdAndStatus(Long scheduleId, SeatStatus status);

    List<Seat> findAllByScheduleId(Long scheduleId);

    List<Seat> findAllByScheduleIdAndStatusIn(Long scheduleId, List<SeatStatus> statuses);

    Optional<Seat> findById(Long seatId);

    Seat save(Seat seat);
}
