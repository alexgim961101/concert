package com.example.concert.domain.concert.repository;

import com.example.concert.domain.concert.entity.ConcertSchedule;
import java.util.List;

public interface ConcertScheduleRepository {
    List<ConcertSchedule> findByConcertId(Long concertId);

    boolean existsConcertById(Long concertId);

    boolean existsById(Long scheduleId);
}
