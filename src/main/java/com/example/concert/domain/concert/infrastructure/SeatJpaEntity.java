package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.SeatStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats", indexes = {
        @Index(name = "idx_seats_schedule_status", columnList = "schedule_id, status")
})
@EntityListeners(AuditingEntityListener.class)
public class SeatJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ConcertScheduleJpaEntity schedule;

    @Column(nullable = false)
    private Integer seatNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Version
    private Long version;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    protected SeatJpaEntity() {
    }

    public SeatJpaEntity(ConcertScheduleJpaEntity schedule, Integer seatNumber, BigDecimal price) {
        this.schedule = schedule;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    // 테스트용 생성자 (scheduleId 직접 사용)
    public SeatJpaEntity(Long scheduleId, Integer seatNumber, BigDecimal price, SeatStatus status) {
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public ConcertScheduleJpaEntity getSchedule() {
        return schedule;
    }

    public Long getScheduleId() {
        return schedule != null ? schedule.getId() : null;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
