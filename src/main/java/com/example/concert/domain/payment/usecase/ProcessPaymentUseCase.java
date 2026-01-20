package com.example.concert.domain.payment.usecase;

import com.example.concert.domain.concert.entity.Seat;
import com.example.concert.domain.concert.repository.SeatRepository;
import com.example.concert.domain.payment.entity.Payment;
import com.example.concert.domain.payment.repository.PaymentRepository;
import com.example.concert.domain.point.usecase.UsePointUseCase;
import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import com.example.concert.domain.queue.usecase.ValidateTokenUseCase;
import com.example.concert.domain.reservation.entity.Reservation;
import com.example.concert.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 예약 결제 처리 UseCase (Facade)
 * 여러 도메인을 조율하여 결제 프로세스를 완성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {
    private final ValidateTokenUseCase validateTokenUseCase;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UsePointUseCase usePointUseCase;
    private final PaymentRepository paymentRepository;
    private final QueueTokenRepository queueTokenRepository;

    @Transactional
    public PaymentResult execute(String token, Long userId, Long reservationId) {
        // 1. 대기열 토큰 검증
        QueueToken queueToken = validateTokenUseCase.execute(token);

        // 2. 예약 조회 및 검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // 본인 예약인지 확인
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예약만 결제할 수 있습니다.");
        }

        // 3. 좌석 조회 및 가격 확인
        Seat seat = seatRepository.findById(reservation.getSeatId())
                .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다."));

        // 4. 포인트 사용 (잔액 차감)
        usePointUseCase.execute(userId, seat.getPrice());

        // 5. 결제 정보 생성 및 저장
        Payment payment = Payment.create(reservationId, userId, seat.getPrice());
        Payment savedPayment = paymentRepository.save(payment);

        // 6. 예약 확정 (PENDING -> CONFIRMED)
        reservation.confirm();
        reservationRepository.save(reservation);

        // 7. 좌석 확정 (TEMP_RESERVED -> RESERVED)
        seat.confirm();
        seatRepository.save(seat);

        // 8. 토큰 만료 처리
        queueToken.expire();
        queueTokenRepository.save(queueToken);

        log.info("Payment completed: paymentId={}, userId={}, amount={}",
                savedPayment.getId(), userId, seat.getPrice());

        return new PaymentResult(
                savedPayment.getId(),
                savedPayment.getStatus().name(),
                seat.getPrice(),
                savedPayment.getCreatedAt());
    }

    public record PaymentResult(
            Long paymentId,
            String status,
            java.math.BigDecimal amount,
            LocalDateTime paidAt) {
    }
}
