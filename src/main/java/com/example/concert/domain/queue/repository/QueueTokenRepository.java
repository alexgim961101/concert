package com.example.concert.domain.queue.repository;

import com.example.concert.domain.queue.entity.QueueToken;
import java.util.Optional;

public interface QueueTokenRepository {
    Optional<QueueToken> findByToken(String token);
}
