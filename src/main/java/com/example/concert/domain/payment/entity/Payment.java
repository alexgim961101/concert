package com.example.concert.domain.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 도메인 객체 (순수 Java, No JPA)
 */
public class Payment {
    private final Long id;
    private final Long reservationId;
    private final Long userId;
    private final BigDecimal amount;
    private PaymentStatus status;
    private final LocalDateTime createdAt;

    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }

    private Payment(Long id, Long reservationId, Long userId, BigDecimal amount, PaymentStatus status,
            LocalDateTime createdAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * 신규 결제 생성 (COMPLETED 상태)
     */
    public static Payment create(Long reservationId, Long userId, BigDecimal amount) {
        return new Payment(null, reservationId, userId, amount, PaymentStatus.COMPLETED, LocalDateTime.now());
    }

    /**
     * 기존 결제 복원 (인프라스트럭처 계층에서 사용)
     */
    public static Payment restore(Long id, Long reservationId, Long userId, BigDecimal amount, PaymentStatus status,
            LocalDateTime createdAt) {
        return new Payment(id, reservationId, userId, amount, status, createdAt);
    }

    public Long getId() {
        return id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
