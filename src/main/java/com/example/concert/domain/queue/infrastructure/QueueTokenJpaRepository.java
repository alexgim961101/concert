package com.example.concert.domain.queue.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QueueTokenJpaRepository extends JpaRepository<QueueTokenJpaEntity, Long> {
    Optional<QueueTokenJpaEntity> findByToken(String token);
}
