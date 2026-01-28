package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidateTokenUseCase {
    private final QueueTokenRepository queueTokenRepository;

    public QueueToken execute(String token) {
        if (token == null || token.isBlank()) {
            throw new TokenNotFoundException("null or empty");
        }

        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenNotFoundException(token));

        if (!queueToken.isActive()) {
            throw new TokenNotActiveException(token);
        }

        if (queueToken.isExpired()) {
            throw new TokenExpiredException(token);
        }

        return queueToken;
    }
}
