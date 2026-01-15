package com.example.concert.domain.queue.infrastructure;

import com.example.concert.domain.queue.entity.QueueToken;
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
}
