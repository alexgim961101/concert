package com.example.concert.domain.concert.infrastructure;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "concert_schedules")
@EntityListeners(AuditingEntityListener.class)
public class ConcertScheduleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private ConcertJpaEntity concert;

    @Column(nullable = false)
    private LocalDateTime concertDate;

    @Column(nullable = false)
    private LocalDateTime reservationStartAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    protected ConcertScheduleJpaEntity() {
    }

    public ConcertScheduleJpaEntity(ConcertJpaEntity concert, LocalDateTime concertDate,
            LocalDateTime reservationStartAt) {
        this.concert = concert;
        this.concertDate = concertDate;
        this.reservationStartAt = reservationStartAt;
    }

    public Long getId() {
        return id;
    }

    public ConcertJpaEntity getConcert() {
        return concert;
    }

    public Long getConcertId() {
        return concert != null ? concert.getId() : null;
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

    public void setId(Long id) {
        this.id = id;
    }
}
