package com.example.concert.domain.payment.infrastructure;

import com.example.concert.domain.payment.entity.Payment;

public class PaymentMapper {

    public static Payment toDomain(PaymentJpaEntity entity) {
        return Payment.restore(
                entity.getId(),
                entity.getReservationId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt());
    }

    public static PaymentJpaEntity toEntity(Payment domain) {
        return new PaymentJpaEntity(
                domain.getReservationId(),
                domain.getUserId(),
                domain.getAmount(),
                domain.getStatus());
    }
}
