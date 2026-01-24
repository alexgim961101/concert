package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatJpaRepository extends JpaRepository<SeatJpaEntity, Long> {
    @Query("SELECT COUNT(s) FROM SeatJpaEntity s WHERE s.schedule.id = :scheduleId AND s.status = :status")
    int countByScheduleIdAndStatus(@Param("scheduleId") Long scheduleId, @Param("status") SeatStatus status);

    @Query("SELECT s FROM SeatJpaEntity s WHERE s.schedule.id = :scheduleId")
    List<SeatJpaEntity> findByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT s FROM SeatJpaEntity s WHERE s.schedule.id = :scheduleId AND s.status IN :statuses")
    List<SeatJpaEntity> findByScheduleIdAndStatusIn(@Param("scheduleId") Long scheduleId,
            @Param("statuses") List<SeatStatus> statuses);

    /**
     * 비관적 락을 사용한 좌석 조회 (동시 예약 방지)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatJpaEntity s WHERE s.id = :id")
    Optional<SeatJpaEntity> findByIdWithLock(@Param("id") Long id);
}
