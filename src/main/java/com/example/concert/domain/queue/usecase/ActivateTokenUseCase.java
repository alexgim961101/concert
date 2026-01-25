package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.concert.infrastructure.ConcertJpaRepository;
import com.example.concert.domain.concert.infrastructure.ConcertJpaEntity;
import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivateTokenUseCase {
    private static final int MAX_ACTIVE_TOKENS_PER_CONCERT = 50;

    private final QueueTokenRepository queueTokenRepository;
    private final ConcertJpaRepository concertJpaRepository;

    @Transactional
    public int execute() {
        List<ConcertJpaEntity> concerts = concertJpaRepository.findAll();
        int totalActivated = 0;

        for (ConcertJpaEntity concert : concerts) {
            long activeCount = queueTokenRepository.countByStatusAndConcertId(
                    TokenStatus.ACTIVE, concert.getId());
            int slotsAvailable = (int) (MAX_ACTIVE_TOKENS_PER_CONCERT - activeCount);

            if (slotsAvailable > 0) {
                List<QueueToken> waitingTokens = queueTokenRepository
                        .findTopNByStatusAndConcertIdOrderByIdAsc(
                                TokenStatus.WAITING, concert.getId(), slotsAvailable);

                for (QueueToken token : waitingTokens) {
                    token.activate();
                    queueTokenRepository.save(token);
                    totalActivated++;
                }
            }
        }
        return totalActivated;
    }
}
