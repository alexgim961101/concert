package com.example.concert.domain.payment.event;

import com.example.concert.common.event.OutboxEvent;
import com.example.concert.common.event.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 결제 이벤트를 Outbox 테이블에 저장하는 Publisher.
 * 비즈니스 트랜잭션과 함께 저장되어 원자성을 보장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 결제 완료 이벤트를 Outbox 테이블에 저장.
     * 실제 Kafka 발행은 OutboxPublisher 스케줄러가 처리합니다.
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.create(
                    PaymentCompletedEvent.AGGREGATE_TYPE,
                    String.valueOf(event.paymentId()),
                    PaymentCompletedEvent.EVENT_TYPE,
                    PaymentCompletedEvent.TOPIC,
                    payload);

            outboxEventRepository.save(outboxEvent);

            log.debug("Saved PaymentCompletedEvent to outbox: paymentId={}, userId={}",
                    event.paymentId(), event.userId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentCompletedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
