package com.example.concert.domain.queue.infrastructure;

import com.example.concert.domain.queue.entity.TokenStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_tokens")
@EntityListeners(AuditingEntityListener.class)
public class QueueTokenJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long concertId;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status = TokenStatus.WAITING;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected QueueTokenJpaEntity() {
    }

    public QueueTokenJpaEntity(Long userId, Long concertId, String token, TokenStatus status, LocalDateTime expiresAt) {
        this.userId = userId;
        this.concertId = concertId;
        this.token = token;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getConcertId() {
        return concertId;
    }

    public String getToken() {
        return token;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(TokenStatus status) {
        this.status = status;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
