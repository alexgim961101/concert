package com.example.concert.domain.payment.repository;

import com.example.concert.domain.payment.entity.Payment;

/**
 * 결제 리포지토리 인터페이스 (도메인 계층)
 */
public interface PaymentRepository {
    Payment save(Payment payment);
}
