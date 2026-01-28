package com.example.concert.domain.queue.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 대기열 토큰 도메인 객체 (순수 Java, No JPA)
 * 
 * Redis 기반 구현에서는 score(epochMillis)를 사용하여 Sorted Set의 순위를 계산합니다.
 * id 필드는 하위 호환성을 위해 유지되지만, Redis에서는 사용되지 않습니다.
 */
public class QueueToken {
    private final Long id;
    private final Long score; // Redis ZSET score (epochMillis), used for ranking
    private final Long userId;
    private final Long concertId;
    private final String token;
    private TokenStatus status;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;

    public QueueToken(Long userId, Long concertId, LocalDateTime expiresAt) {
        this(null, System.currentTimeMillis(), userId, concertId,
                UUID.randomUUID().toString(), TokenStatus.WAITING, expiresAt, null);
    }

    /**
     * Legacy constructor for backward compatibility (id-based)
     */
    public QueueToken(Long id, Long userId, Long concertId, String token,
            TokenStatus status, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this(id, null, userId, concertId, token, status, expiresAt, createdAt);
    }

    /**
     * Full constructor with score for Redis-based implementation
     */
    public QueueToken(Long id, Long score, Long userId, Long concertId, String token,
            TokenStatus status, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.id = id;
        this.score = score;
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

    public Long getScore() {
        return score;
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
