package com.example.concert.domain.point.usecase;

/**
 * PG 결제 실패 예외
 */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}
