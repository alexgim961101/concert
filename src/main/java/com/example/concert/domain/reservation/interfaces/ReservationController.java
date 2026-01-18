package com.example.concert.domain.reservation.interfaces;

import com.example.concert.domain.reservation.usecase.ReserveSeatUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

        private final ReserveSeatUseCase reserveSeatUseCase;

        @PostMapping
        public ResponseEntity<ReservationResponse> reserveSeat(
                        @RequestHeader("Concert-Queue-Token") String token,
                        @Valid @RequestBody ReservationRequest request) {

                ReserveSeatUseCase.ReservationResult result = reserveSeatUseCase.execute(
                                token,
                                request.userId(),
                                request.scheduleId(),
                                request.seatId());

                return ResponseEntity.ok(new ReservationResponse(
                                result.reservationId(),
                                result.status().name(),
                                result.expiresAt()));
        }

        // ===== DTOs =====
        public record ReservationRequest(
                        @NotNull(message = "userId는 필수입니다") Long userId,
                        @NotNull(message = "scheduleId는 필수입니다") Long scheduleId,
                        @NotNull(message = "seatId는 필수입니다") Long seatId) {
        }

        public record ReservationResponse(
                        Long reservationId,
                        String status,
                        LocalDateTime expiresAt) {
        }
}
