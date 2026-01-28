package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IssueTokenUseCase {
    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final int ESTIMATED_PROCESSING_TIME_PER_USER_SECONDS = 2;
    private static final int MAX_ACTIVE_TOKENS_PER_CONCERT = 50;

    private final QueueTokenRepository queueTokenRepository;

    public IssueTokenResult execute(Long userId, Long concertId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (concertId == null) {
            throw new IllegalArgumentException("concertId cannot be null");
        }

        long activeCount = queueTokenRepository.countByStatusAndConcertId(TokenStatus.ACTIVE, concertId);
        boolean shouldActivateImmediately = activeCount < MAX_ACTIVE_TOKENS_PER_CONCERT;

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);
        QueueToken queueToken = new QueueToken(userId, concertId, expiresAt);

        if (shouldActivateImmediately) {
            queueToken.activate();
        }

        QueueToken savedToken = queueTokenRepository.save(queueToken);

        long rank = 0;
        if (savedToken.getStatus() == TokenStatus.WAITING) {
            // Redis ZRANK로 순위 조회 (0-indexed → 1-indexed)
            Long zrank = queueTokenRepository.getRankByToken(savedToken.getToken(), concertId);
            rank = zrank != null ? zrank + 1 : 1;
        }

        long estimatedWaitTime = rank * ESTIMATED_PROCESSING_TIME_PER_USER_SECONDS;

        return new IssueTokenResult(
                savedToken.getToken(),
                savedToken.getStatus().name(),
                rank,
                estimatedWaitTime);
    }

    public record IssueTokenResult(
            String token,
            String status,
            long rank,
            long estimatedWaitTime) {
    }
}
