package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.Seat;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SeatRepositoryImpl implements SeatRepository {
    private final SeatJpaRepository jpaRepository;

    @Override
    public int countByScheduleIdAndStatus(Long scheduleId, SeatStatus status) {
        return jpaRepository.countByScheduleIdAndStatus(scheduleId, status);
    }

    @Override
    public List<Seat> findAllByScheduleId(Long scheduleId) {
        return jpaRepository.findByScheduleId(scheduleId).stream()
                .map(SeatMapper::toDomain)
                .toList();
    }

    @Override
    public List<Seat> findAllByScheduleIdAndStatusIn(Long scheduleId, List<SeatStatus> statuses) {
        return jpaRepository.findByScheduleIdAndStatusIn(scheduleId, statuses).stream()
                .map(SeatMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Seat> findById(Long seatId) {
        return jpaRepository.findById(seatId)
                .map(SeatMapper::toDomain);
    }

    @Override
    public Seat save(Seat seat) {
        SeatJpaEntity entity = jpaRepository.findById(seat.getId())
                .orElseThrow(() -> new IllegalArgumentException("Seat not found for update: " + seat.getId()));
        entity.setStatus(seat.getStatus());
        SeatJpaEntity saved = jpaRepository.save(entity);
        return SeatMapper.toDomain(saved);
    }
}
