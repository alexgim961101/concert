package com.example.concert.common.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Transactional Outbox 패턴용 이벤트 엔티티.
 * 비즈니스 트랜잭션과 함께 저장되어 원자성을 보장합니다.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status", columnList = "status"),
        @Index(name = "idx_outbox_created_at", columnList = "createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // e.g., "Payment"

    @Column(nullable = false)
    private String aggregateId; // e.g., 결제 ID

    @Column(nullable = false)
    private String eventType; // e.g., "PaymentCompleted"

    @Column(nullable = false)
    private String topic; // Kafka topic name

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON 직렬화된 이벤트 데이터

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime publishedAt;

    @Column
    private Integer retryCount;

    @Column
    private String lastError;

    private OutboxEvent(String aggregateType, String aggregateId, String eventType,
            String topic, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }

    public static OutboxEvent create(String aggregateType, String aggregateId,
            String eventType, String topic, String payload) {
        return new OutboxEvent(aggregateType, aggregateId, eventType, topic, payload);
    }

    public void markAsPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = OutboxEventStatus.FAILED;
        this.retryCount++;
        this.lastError = error;
    }

    public void resetToPending() {
        this.status = OutboxEventStatus.PENDING;
    }
}
