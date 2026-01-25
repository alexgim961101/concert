package com.example.concert.domain.queue.infrastructure;

import com.example.concert.domain.queue.usecase.ActivateTokenUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {
    private final ActivateTokenUseCase activateTokenUseCase;

    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    public void activateWaitingTokens() {
        log.debug("Running queue activation scheduler");
        int activated = activateTokenUseCase.execute();
        if (activated > 0) {
            log.info("Queue activation scheduler completed: {} tokens activated", activated);
        }
    }
}
