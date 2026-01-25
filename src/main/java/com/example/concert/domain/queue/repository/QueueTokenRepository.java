package com.example.concert.domain.queue.repository;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import java.util.Optional;

public interface QueueTokenRepository {
    Optional<QueueToken> findByToken(String token);

    QueueToken save(QueueToken queueToken);

    long countByStatusAndIdLessThan(TokenStatus status, Long id);

    long countByStatusAndConcertIdAndIdLessThan(TokenStatus status, Long concertId, Long id);

    long countByStatusAndConcertId(TokenStatus status, Long concertId);

    java.util.List<QueueToken> findTopNByStatusAndConcertIdOrderByIdAsc(TokenStatus status, Long concertId, int limit);
}
