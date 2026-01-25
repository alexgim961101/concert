package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTokenStatusUseCase {
        private static final int ESTIMATED_PROCESSING_TIME_PER_USER_SECONDS = 2;

        private final QueueTokenRepository queueTokenRepository;

        public TokenStatusResult execute(String token) {
                if (token == null || token.isBlank()) {
                        throw new TokenNotFoundException("null or empty");
                }

                QueueToken queueToken = queueTokenRepository.findByToken(token)
                                .orElseThrow(() -> new TokenNotFoundException(token));

                return switch (queueToken.getStatus()) {
                        case ACTIVE -> new TokenStatusResult(
                                        queueToken.getToken(),
                                        TokenStatus.ACTIVE.name(),
                                        0,
                                        0);
                        case EXPIRED -> new TokenStatusResult(
                                        queueToken.getToken(),
                                        TokenStatus.EXPIRED.name(),
                                        0,
                                        0);
                        case WAITING -> {
                                long rank = queueTokenRepository.countByStatusAndConcertIdAndIdLessThan(
                                                TokenStatus.WAITING, queueToken.getConcertId(), queueToken.getId()) + 1;
                                long estimatedWaitTime = rank * ESTIMATED_PROCESSING_TIME_PER_USER_SECONDS;
                                yield new TokenStatusResult(
                                                queueToken.getToken(),
                                                TokenStatus.WAITING.name(),
                                                rank,
                                                estimatedWaitTime);
                        }
                };
        }

        public record TokenStatusResult(
                        String token,
                        String status,
                        long rank,
                        long estimatedWaitTime) {
        }
}
