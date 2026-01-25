package com.example.concert.domain.queue.infrastructure;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QueueTokenRepositoryImpl implements QueueTokenRepository {
    private final QueueTokenJpaRepository jpaRepository;

    @Override
    public Optional<QueueToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(QueueTokenMapper::toDomain);
    }

    @Override
    public QueueToken save(QueueToken queueToken) {
        QueueTokenJpaEntity entity;
        if (queueToken.getId() != null) {
            entity = jpaRepository.findById(queueToken.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Token not found: " + queueToken.getId()));
            entity.setStatus(queueToken.getStatus());
        } else {
            entity = QueueTokenMapper.toEntity(queueToken);
        }
        QueueTokenJpaEntity saved = jpaRepository.save(entity);
        return QueueTokenMapper.toDomain(saved);
    }

    @Override
    public long countByStatusAndIdLessThan(TokenStatus status, Long id) {
        return jpaRepository.countByStatusAndIdLessThan(status, id);
    }

    @Override
    public long countByStatusAndConcertIdAndIdLessThan(TokenStatus status, Long concertId, Long id) {
        return jpaRepository.countByStatusAndConcertIdAndIdLessThan(status, concertId, id);
    }

    @Override
    public long countByStatusAndConcertId(TokenStatus status, Long concertId) {
        return jpaRepository.countByStatusAndConcertId(status, concertId);
    }

    @Override
    public java.util.List<QueueToken> findTopNByStatusAndConcertIdOrderByIdAsc(TokenStatus status, Long concertId,
            int limit) {
        return jpaRepository.findTopByStatusAndConcertIdOrderByIdAsc(
                status, concertId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(QueueTokenMapper::toDomain)
                .toList();
    }
}
