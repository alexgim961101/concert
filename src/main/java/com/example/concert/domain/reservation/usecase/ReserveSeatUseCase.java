package com.example.concert.domain.reservation.usecase;

import com.example.concert.domain.concert.entity.Seat;
import com.example.concert.domain.concert.repository.ConcertScheduleRepository;
import com.example.concert.domain.concert.repository.SeatRepository;
import com.example.concert.domain.queue.usecase.ValidateTokenUseCase;
import com.example.concert.domain.reservation.entity.Reservation;
import com.example.concert.domain.reservation.entity.ReservationStatus;
import com.example.concert.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReserveSeatUseCase {
    private final ValidateTokenUseCase validateTokenUseCase;
    private final ConcertScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ReservationResult execute(String token, Long userId, Long scheduleId, Long seatId) {
        // 1. 토큰 검증 (기존 concert 로직과 동일)
        validateTokenUseCase.execute(token);

        // 2. 스케줄 존재 확인
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ScheduleNotFoundException(scheduleId);
        }

        // 3. 좌석 조회
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        // 4. 좌석 예약 (도메인 로직)
        try {
            seat.reserve();
        } catch (IllegalStateException e) {
            throw new SeatNotAvailableException(seatId);
        }

        // 5. 좌석 상태 저장
        seatRepository.save(seat);

        // 6. 예약 생성 및 저장
        Reservation reservation = Reservation.create(userId, scheduleId, seatId);
        Reservation saved = reservationRepository.save(reservation);

        return new ReservationResult(
                saved.getId(),
                saved.getStatus(),
                saved.getExpiresAt());
    }

    public record ReservationResult(
            Long reservationId,
            ReservationStatus status,
            LocalDateTime expiresAt) {
    }
}
