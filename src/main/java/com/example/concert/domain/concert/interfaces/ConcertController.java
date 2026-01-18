package com.example.concert.domain.concert.interfaces;

import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.usecase.GetAvailableDatesUseCase;
import com.example.concert.domain.concert.usecase.GetSeatsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConcertController {

    private final GetAvailableDatesUseCase getAvailableDatesUseCase;
    private final GetSeatsUseCase getSeatsUseCase;

    // ===== 콘서트 스케줄 조회 =====
    @GetMapping("/concerts/{concertId}/schedules")
    public ResponseEntity<ConcertSchedulesResponse> getAvailableSchedules(
            @PathVariable Long concertId,
            @RequestHeader("Concert-Queue-Token") String token) {

        GetAvailableDatesUseCase.AvailableDatesResult result = getAvailableDatesUseCase.execute(token, concertId);

        List<ScheduleResponse> schedules = result.schedules().stream()
                .map(s -> new ScheduleResponse(s.id(), s.date(), s.availableSeats()))
                .toList();

        return ResponseEntity.ok(new ConcertSchedulesResponse(result.concertId(), schedules));
    }

    // ===== 좌석 조회 =====
    @GetMapping("/schedules/{scheduleId}/seats")
    public ResponseEntity<SeatsResponse> getSeats(
            @PathVariable Long scheduleId,
            @RequestHeader("Concert-Queue-Token") String token,
            @RequestParam(required = false) List<SeatStatus> status) {

        GetSeatsUseCase.SeatsResult result = getSeatsUseCase.execute(token, scheduleId, status);

        List<SeatResponse> seats = result.seats().stream()
                .map(s -> new SeatResponse(s.id(), s.number(), s.status().name(), s.price()))
                .toList();

        return ResponseEntity.ok(new SeatsResponse(result.scheduleId(), seats));
    }

    // ===== DTOs =====
    public record ScheduleResponse(Long id, LocalDate date, int availableSeats) {
    }

    public record ConcertSchedulesResponse(Long concertId, List<ScheduleResponse> schedules) {
    }

    public record SeatResponse(Long id, Integer number, String status, BigDecimal price) {
    }

    public record SeatsResponse(Long scheduleId, List<SeatResponse> seats) {
    }
}
