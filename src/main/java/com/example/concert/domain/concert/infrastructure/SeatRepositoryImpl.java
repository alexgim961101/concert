package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SeatRepositoryImpl implements SeatRepository {
    private final SeatJpaRepository jpaRepository;

    @Override
    public int countByScheduleIdAndStatus(Long scheduleId, SeatStatus status) {
        return jpaRepository.countByScheduleIdAndStatus(scheduleId, status);
    }
}
