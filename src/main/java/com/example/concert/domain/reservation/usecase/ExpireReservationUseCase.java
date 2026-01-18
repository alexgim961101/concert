package com.example.concert.domain.reservation.usecase;

import com.example.concert.domain.concert.entity.Seat;
import com.example.concert.domain.concert.repository.SeatRepository;
import com.example.concert.domain.reservation.entity.Reservation;
import com.example.concert.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 만료된 예약을 처리하는 UseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpireReservationUseCase {
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public int execute() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findAllExpired(now);

        if (expiredReservations.isEmpty()) {
            return 0;
        }

        log.info("Found {} expired reservations to process", expiredReservations.size());

        int processedCount = 0;
        for (Reservation reservation : expiredReservations) {
            try {
                // 1. 예약 상태 -> EXPIRED
                reservation.expire();
                reservationRepository.save(reservation);

                // 2. 좌석 상태 -> AVAILABLE
                Seat seat = seatRepository.findById(reservation.getSeatId())
                        .orElse(null);
                if (seat != null) {
                    seat.release();
                    seatRepository.save(seat);
                }

                processedCount++;
                log.debug("Expired reservation: id={}, seatId={}", reservation.getId(), reservation.getSeatId());
            } catch (Exception e) {
                log.error("Failed to expire reservation: id={}", reservation.getId(), e);
            }
        }

        log.info("Processed {} expired reservations", processedCount);
        return processedCount;
    }
}
