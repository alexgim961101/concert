package com.example.concert.domain.reservation.infrastructure;

import com.example.concert.domain.reservation.entity.Reservation;
import com.example.concert.domain.reservation.entity.ReservationStatus;
import com.example.concert.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {
    private final ReservationJpaRepository jpaRepository;

    @Override
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity entity;
        if (reservation.getId() != null) {
            // 기존 예약 업데이트
            entity = jpaRepository.findById(reservation.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservation.getId()));
            entity.setStatus(reservation.getStatus());
        } else {
            // 신규 예약 생성
            entity = ReservationMapper.toEntity(reservation);
        }
        ReservationJpaEntity saved = jpaRepository.save(entity);
        return ReservationMapper.toDomain(saved);
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return jpaRepository.findById(id)
                .map(ReservationMapper::toDomain);
    }

    @Override
    public Optional<Reservation> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id)
                .map(ReservationMapper::toDomain);
    }

    @Override
    public List<Reservation> findAllExpired(LocalDateTime now) {
        return jpaRepository.findAllByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now)
                .stream()
                .map(ReservationMapper::toDomain)
                .toList();
    }
}
