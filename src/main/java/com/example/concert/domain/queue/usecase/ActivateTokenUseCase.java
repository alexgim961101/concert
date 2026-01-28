package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivateTokenUseCase {
    private static final int MAX_ACTIVE_TOKENS_PER_CONCERT = 50;

    private final QueueTokenRepository queueTokenRepository;

    public int execute() {
        // Redis 키 스캔으로 대기열이 있는 콘서트 ID 조회
        Set<Long> concertIds = queueTokenRepository.getActiveConcertIds();
        int totalActivated = 0;

        for (Long concertId : concertIds) {
            long activeCount = queueTokenRepository.countByStatusAndConcertId(
                    TokenStatus.ACTIVE, concertId);
            int slotsAvailable = (int) (MAX_ACTIVE_TOKENS_PER_CONCERT - activeCount);

            if (slotsAvailable > 0) {
                List<QueueToken> waitingTokens = queueTokenRepository
                        .findTopNByStatusAndConcertIdOrderByIdAsc(
                                TokenStatus.WAITING, concertId, slotsAvailable);

                for (QueueToken token : waitingTokens) {
                    token.activate();
                    queueTokenRepository.save(token);
                    totalActivated++;
                }

                if (!waitingTokens.isEmpty()) {
                    log.debug("Activated {} tokens for concert {}", waitingTokens.size(), concertId);
                }
            }
        }
        return totalActivated;
    }
}
