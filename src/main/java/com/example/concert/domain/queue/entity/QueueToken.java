package com.example.concert.domain.queue.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 대기열 토큰 도메인 객체 (순수 Java, No JPA)
 */
public class QueueToken {
    private final Long id;
    private final Long userId;
    private final Long concertId;
    private final String token;
    private TokenStatus status;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;

    public QueueToken(Long userId, Long concertId, LocalDateTime expiresAt) {
        this(null, userId, concertId, UUID.randomUUID().toString(), TokenStatus.WAITING, expiresAt, null);
    }

    public QueueToken(Long id, Long userId, Long concertId, String token,
            TokenStatus status, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.concertId = concertId;
        this.token = token;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return this.status == TokenStatus.ACTIVE;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) || this.status == TokenStatus.EXPIRED;
    }

    public boolean isValid() {
        return isActive() && !isExpired();
    }

    public void activate() {
        if (this.status != TokenStatus.WAITING) {
            throw new IllegalStateException("Token is not in waiting state");
        }
        this.status = TokenStatus.ACTIVE;
    }

    public void expire() {
        this.status = TokenStatus.EXPIRED;
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
}
