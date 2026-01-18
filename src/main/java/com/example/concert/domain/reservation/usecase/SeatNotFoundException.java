package com.example.concert.domain.reservation.usecase;

/**
 * 좌석을 찾을 수 없을 때 발생하는 예외
 */
public class SeatNotFoundException extends RuntimeException {
    public SeatNotFoundException(Long seatId) {
        super("Seat not found: " + seatId);
    }
}
