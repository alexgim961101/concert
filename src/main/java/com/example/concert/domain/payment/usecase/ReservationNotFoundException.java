package com.example.concert.domain.payment.usecase;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long reservationId) {
        super("예약을 찾을 수 없습니다. id=" + reservationId);
    }
}
