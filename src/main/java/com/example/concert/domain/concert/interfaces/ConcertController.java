package com.example.concert.domain.concert.interfaces;

import com.example.concert.domain.concert.usecase.GetAvailableDatesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
public class ConcertController {
    private final GetAvailableDatesUseCase getAvailableDatesUseCase;

    @GetMapping("/{concertId}/schedules")
    public ResponseEntity<ConcertSchedulesResponse> getAvailableSchedules(
            @PathVariable Long concertId,
            @RequestHeader("Authorization") String token) {

        GetAvailableDatesUseCase.AvailableDatesResult result = getAvailableDatesUseCase.execute(token, concertId);

        List<ScheduleResponse> schedules = result.schedules().stream()
                .map(s -> new ScheduleResponse(s.id(), s.date(), s.availableSeats()))
                .toList();

        return ResponseEntity.ok(new ConcertSchedulesResponse(result.concertId(), schedules));
    }

    public record ScheduleResponse(Long id, java.time.LocalDate date, int availableSeats) {
    }

    public record ConcertSchedulesResponse(Long concertId, List<ScheduleResponse> schedules) {
    }
}
