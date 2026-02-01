package com.example.concert.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 테이블에서 미발행 이벤트를 읽어 Kafka로 발행하는 스케줄러.
 * Transactional Outbox 패턴의 발행 부분을 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int MAX_RETRY_COUNT = 5;
    private static final int CLEANUP_DAYS = 7;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 1초마다 미발행 이벤트를 Kafka로 발행
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish event: id={}, topic={}, error={}",
                                        event.getId(), event.getTopic(), ex.getMessage());
                            }
                        });

                event.markAsPublished();
                outboxEventRepository.save(event);

                log.debug("Published outbox event: id={}, topic={}, aggregateId={}",
                        event.getId(), event.getTopic(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Error publishing outbox event: id={}, error={}",
                        event.getId(), e.getMessage(), e);
                event.markAsFailed(e.getMessage());
                outboxEventRepository.save(event);
            }
        }

        if (!pendingEvents.isEmpty()) {
            log.info("Published {} outbox events", pendingEvents.size());
        }
    }

    /**
     * 5분마다 실패한 이벤트 재시도
     */
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxEventRepository
                .findFailedEventsForRetry(OutboxEventStatus.FAILED, MAX_RETRY_COUNT);

        for (OutboxEvent event : failedEvents) {
            event.resetToPending();
            outboxEventRepository.save(event);
        }

        if (!failedEvents.isEmpty()) {
            log.info("Reset {} failed events for retry", failedEvents.size());
        }
    }

    /**
     * 매일 자정에 오래된 발행 완료 이벤트 정리
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(CLEANUP_DAYS);
        int deleted = outboxEventRepository.deleteByStatusAndPublishedAtBefore(
                OutboxEventStatus.PUBLISHED, cutoff);

        if (deleted > 0) {
            log.info("Cleaned up {} old outbox events", deleted);
        }
    }
}
