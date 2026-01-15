package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SeatJpaRepository extends JpaRepository<SeatJpaEntity, Long> {
    @Query("SELECT COUNT(s) FROM SeatJpaEntity s WHERE s.schedule.id = :scheduleId AND s.status = :status")
    int countByScheduleIdAndStatus(@Param("scheduleId") Long scheduleId, @Param("status") SeatStatus status);

    @Query("SELECT s FROM SeatJpaEntity s WHERE s.schedule.id = :scheduleId")
    List<SeatJpaEntity> findByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT s FROM SeatJpaEntity s WHERE s.schedule.id = :scheduleId AND s.status IN :statuses")
    List<SeatJpaEntity> findByScheduleIdAndStatusIn(@Param("scheduleId") Long scheduleId,
            @Param("statuses") List<SeatStatus> statuses);
}
