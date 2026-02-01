package com.example.concert.common.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 이벤트 Repository
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * 발행 대기 중인 이벤트 조회 (생성 시간순, 상위 N개)
     */
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    /**
     * 특정 시간 이전에 생성된 발행 완료 이벤트 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = :status AND e.publishedAt < :before")
    int deleteByStatusAndPublishedAtBefore(
            @Param("status") OutboxEventStatus status,
            @Param("before") LocalDateTime before);

    /**
     * 실패한 이벤트 중 재시도 횟수가 임계값 이하인 것 조회
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status AND e.retryCount < :maxRetry ORDER BY e.createdAt ASC")
    List<OutboxEvent> findFailedEventsForRetry(
            @Param("status") OutboxEventStatus status,
            @Param("maxRetry") int maxRetry);
}
