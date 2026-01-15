package com.example.concert.domain.concert.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 좌석 도메인 객체 (순수 Java, No JPA)
 */
public class Seat {
    private final Long id;
    private final Long scheduleId;
    private final Integer seatNumber;
    private final BigDecimal price;
    private SeatStatus status;
    private final Long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Seat(Long scheduleId, Integer seatNumber, BigDecimal price) {
        this(null, scheduleId, seatNumber, price, SeatStatus.AVAILABLE, null, null, null);
    }

    public Seat(Long id, Long scheduleId, Integer seatNumber, BigDecimal price,
            SeatStatus status, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    public void reserve() {
        if (!isAvailable()) {
            throw new IllegalStateException("Seat is not available for reservation");
        }
        this.status = SeatStatus.TEMP_RESERVED;
    }

    public void confirm() {
        if (this.status != SeatStatus.TEMP_RESERVED) {
            throw new IllegalStateException("Seat is not in temp reserved state");
        }
        this.status = SeatStatus.RESERVED;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public Long getScheduleId() {
        return scheduleId;
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
}
