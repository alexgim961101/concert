package com.example.concert.domain.queue.infrastructure;

import com.example.concert.domain.queue.entity.QueueToken;

public class QueueTokenMapper {
    public static QueueToken toDomain(QueueTokenJpaEntity entity) {
        if (entity == null)
            return null;
        return new QueueToken(
                entity.getId(),
                entity.getUserId(),
                entity.getConcertId(),
                entity.getToken(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt());
    }
}
