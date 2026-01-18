package com.example.concert.domain.reservation.usecase;

/**
 * 좌석이 예약 가능 상태가 아닐 때 발생하는 예외
 */
public class SeatNotAvailableException extends RuntimeException {
    public SeatNotAvailableException(Long seatId) {
        super("Seat is not available for reservation: " + seatId);
    }
}
