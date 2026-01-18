package com.example.concert.domain.reservation.infrastructure;

import com.example.concert.domain.reservation.entity.Reservation;
import com.example.concert.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {
    private final ReservationJpaRepository jpaRepository;

    @Override
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity entity = ReservationMapper.toEntity(reservation);
        ReservationJpaEntity saved = jpaRepository.save(entity);
        return ReservationMapper.toDomain(saved);
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return jpaRepository.findById(id)
                .map(ReservationMapper::toDomain);
    }
}
