package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatJpaRepository extends JpaRepository<SeatJpaEntity, Long> {
    @Query("SELECT COUNT(s) FROM SeatJpaEntity s WHERE s.schedule.id = :scheduleId AND s.status = :status")
    int countByScheduleIdAndStatus(@Param("scheduleId") Long scheduleId, @Param("status") SeatStatus status);
}
