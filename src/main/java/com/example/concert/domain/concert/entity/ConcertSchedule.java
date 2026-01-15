package com.example.concert.domain.concert.entity;

import java.time.LocalDateTime;

/**
 * 콘서트 스케줄 도메인 객체 (순수 Java, No JPA)
 */
public class ConcertSchedule {
    private final Long id;
    private final Long concertId;
    private final LocalDateTime concertDate;
    private final LocalDateTime reservationStartAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ConcertSchedule(Long concertId, LocalDateTime concertDate, LocalDateTime reservationStartAt) {
        this(null, concertId, concertDate, reservationStartAt, null, null);
    }

    public ConcertSchedule(Long id, Long concertId, LocalDateTime concertDate,
            LocalDateTime reservationStartAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.concertId = concertId;
        this.concertDate = concertDate;
        this.reservationStartAt = reservationStartAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isReservationOpen() {
        return LocalDateTime.now().isAfter(reservationStartAt);
    }

    public Long getId() {
        return id;
    }

    public Long getConcertId() {
        return concertId;
    }

    public LocalDateTime getConcertDate() {
        return concertDate;
    }

    public LocalDateTime getReservationStartAt() {
        return reservationStartAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
