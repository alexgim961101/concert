package com.example.concert.domain.reservation.infrastructure;

import com.example.concert.domain.reservation.entity.ReservationStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@EntityListeners(AuditingEntityListener.class)
public class ReservationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long scheduleId;

    @Column(nullable = false)
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected ReservationJpaEntity() {
    }

    public ReservationJpaEntity(Long userId, Long scheduleId, Long seatId,
            ReservationStatus status, LocalDateTime expiresAt) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.seatId = seatId;
        this.status = status;
        this.expiresAt = expiresAt;
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

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
