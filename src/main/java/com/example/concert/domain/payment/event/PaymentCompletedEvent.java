package com.example.concert.domain.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트.
 * Kafka를 통해 발행되어 다른 도메인에서 소비됩니다.
 */
public record PaymentCompletedEvent(
        Long paymentId,
        Long reservationId,
        Long userId,
        String token,
        BigDecimal amount,
        LocalDateTime paidAt) {
    public static final String TOPIC = "payment-completed";
    public static final String AGGREGATE_TYPE = "Payment";
    public static final String EVENT_TYPE = "PaymentCompleted";
}
