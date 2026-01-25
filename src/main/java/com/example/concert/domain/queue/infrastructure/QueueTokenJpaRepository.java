package com.example.concert.domain.queue.infrastructure;

import com.example.concert.domain.queue.entity.TokenStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QueueTokenJpaRepository extends JpaRepository<QueueTokenJpaEntity, Long> {
    Optional<QueueTokenJpaEntity> findByToken(String token);

    long countByStatusAndIdLessThan(TokenStatus status, Long id);

    long countByStatusAndConcertIdAndIdLessThan(TokenStatus status, Long concertId, Long id);

    long countByStatusAndConcertId(TokenStatus status, Long concertId);

    List<QueueTokenJpaEntity> findTopByStatusAndConcertIdOrderByIdAsc(TokenStatus status, Long concertId,
            Pageable pageable);
}
