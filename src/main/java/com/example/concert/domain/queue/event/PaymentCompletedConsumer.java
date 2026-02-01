package com.example.concert.domain.queue.event;

import com.example.concert.domain.payment.event.PaymentCompletedEvent;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 결제 완료 이벤트를 소비하여 토큰을 만료 처리하는 Consumer.
 * 이벤트 기반으로 Queue 도메인이 Payment 도메인과 느슨하게 결합됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final QueueTokenRepository queueTokenRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-completed", groupId = "queue-consumer-group")
    public void handlePaymentCompleted(String message, Acknowledgment ack) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);

            log.info("Received PaymentCompletedEvent: paymentId={}, userId={}, token={}",
                    event.paymentId(), event.userId(), event.token());

            // 토큰 만료 처리
            expireToken(event.token());

            // 수동 ACK
            ack.acknowledge();

            log.info("Successfully processed PaymentCompletedEvent: paymentId={}", event.paymentId());

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize PaymentCompletedEvent: {}", e.getMessage(), e);
            // 역직렬화 실패는 재시도해도 의미 없으므로 ACK 처리
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process PaymentCompletedEvent: {}", e.getMessage(), e);
            // 처리 실패 시 ACK 하지 않음 → DLQ 이동
            throw e;
        }
    }

    private void expireToken(String token) {
        queueTokenRepository.findByToken(token)
                .ifPresentOrElse(
                        queueToken -> {
                            queueToken.expire();
                            queueTokenRepository.save(queueToken);
                            log.debug("Token expired: {}", token);
                        },
                        () -> log.warn("Token not found for expiration: {}", token));
    }
}
