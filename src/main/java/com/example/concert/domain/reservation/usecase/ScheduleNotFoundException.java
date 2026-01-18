package com.example.concert.domain.reservation.usecase;

/**
 * 스케줄을 찾을 수 없을 때 발생하는 예외
 */
public class ScheduleNotFoundException extends RuntimeException {
    public ScheduleNotFoundException(Long scheduleId) {
        super("Schedule not found: " + scheduleId);
    }
}
