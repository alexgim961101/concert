package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.ConcertSchedule;
import com.example.concert.domain.concert.repository.ConcertScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConcertScheduleRepositoryImpl implements ConcertScheduleRepository {
    private final ConcertScheduleJpaRepository jpaRepository;

    @Override
    public List<ConcertSchedule> findByConcertId(Long concertId) {
        return jpaRepository.findByConcertId(concertId).stream()
                .map(ConcertScheduleMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsConcertById(Long concertId) {
        return jpaRepository.existsConcertById(concertId);
    }

    @Override
    public boolean existsById(Long scheduleId) {
        return jpaRepository.existsById(scheduleId);
    }
}
