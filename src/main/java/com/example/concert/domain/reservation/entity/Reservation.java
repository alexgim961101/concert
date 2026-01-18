package com.example.concert.domain.reservation.entity;

import java.time.LocalDateTime;

/**
 * 예약 도메인 객체 (순수 Java, No JPA)
 */
public class Reservation {
    private static final int EXPIRATION_MINUTES = 5;

    private final Long id;
    private final Long userId;
    private final Long scheduleId;
    private final Long seatId;
    private ReservationStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    private Reservation(Long id, Long userId, Long scheduleId, Long seatId,
            ReservationStatus status, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.seatId = seatId;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /**
     * 새로운 예약 생성 팩토리 메서드
     */
    public static Reservation create(Long userId, Long scheduleId, Long seatId) {
        LocalDateTime now = LocalDateTime.now();
        return new Reservation(
                null,
                userId,
                scheduleId,
                seatId,
                ReservationStatus.PENDING,
                now,
                now.plusMinutes(EXPIRATION_MINUTES));
    }

    /**
     * 기존 예약 복원용 생성자 (인프라스트럭처 계층에서 사용)
     */
    public static Reservation restore(Long id, Long userId, Long scheduleId, Long seatId,
            ReservationStatus status, LocalDateTime createdAt, LocalDateTime expiresAt) {
        return new Reservation(id, userId, scheduleId, seatId, status, createdAt, expiresAt);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void confirm() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed");
        }
        if (isExpired()) {
            throw new IllegalStateException("Reservation has expired");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
