package com.example.concert.domain.queue.repository;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QueueTokenRepository {
    Optional<QueueToken> findByToken(String token);

    QueueToken save(QueueToken queueToken);

    long countByStatusAndIdLessThan(TokenStatus status, Long id);

    long countByStatusAndConcertIdAndIdLessThan(TokenStatus status, Long concertId, Long id);

    long countByStatusAndConcertId(TokenStatus status, Long concertId);

    List<QueueToken> findTopNByStatusAndConcertIdOrderByIdAsc(TokenStatus status, Long concertId, int limit);

    /**
     * Redis ZRANK를 통해 토큰의 대기열 순위를 반환 (0-indexed)
     */
    Long getRankByToken(String token, Long concertId);

    /**
     * 대기열이 있는 콘서트 ID 목록을 반환
     */
    Set<Long> getActiveConcertIds();
}
