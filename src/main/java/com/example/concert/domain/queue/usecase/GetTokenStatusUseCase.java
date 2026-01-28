package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
                                // Redis ZRANK로 순위 조회 (0-indexed → 1-indexed)
                                Long zrank = queueTokenRepository.getRankByToken(token, queueToken.getConcertId());
                                long rank = zrank != null ? zrank + 1 : 1;
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
