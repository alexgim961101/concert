package com.example.concert.domain.concert.repository;

import com.example.concert.domain.concert.entity.ConcertSchedule;
import java.util.List;

public interface ConcertScheduleRepository {
    List<ConcertSchedule> findByConcertId(Long concertId);

    boolean existsConcertById(Long concertId);

    boolean existsById(Long scheduleId);

    /**
     * 오늘부터 지정된 일수 내의 공연이 있는 콘서트 ID 목록 조회 (캐시 웜업용)
     */
    List<Long> findUpcomingConcertIds(int daysAhead);
}
