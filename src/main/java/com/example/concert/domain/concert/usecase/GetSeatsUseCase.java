package com.example.concert.domain.concert.usecase;

import com.example.concert.domain.concert.entity.Seat;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.repository.SeatRepository;
import com.example.concert.domain.queue.usecase.ValidateTokenUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSeatsUseCase {

    private final ValidateTokenUseCase validateTokenUseCase;
    private final SeatRepository seatRepository;

    public SeatsResult execute(String token, Long scheduleId, List<SeatStatus> statuses) {
        // 1. 토큰 검증
        validateTokenUseCase.execute(token);

        // 2. 좌석 조회 (상태 필터링 여부에 따라 분기)
        List<Seat> seats;
        if (statuses == null || statuses.isEmpty()) {
            seats = seatRepository.findAllByScheduleId(scheduleId);
        } else {
            seats = seatRepository.findAllByScheduleIdAndStatusIn(scheduleId, statuses);
        }

        // 3. 결과 반환
        List<SeatInfo> seatInfos = seats.stream()
                .map(seat -> new SeatInfo(
                        seat.getId(),
                        seat.getSeatNumber(),
                        seat.getStatus(),
                        seat.getPrice()))
                .toList();

        return new SeatsResult(scheduleId, seatInfos);
    }

    public record SeatInfo(Long id, Integer number, SeatStatus status, BigDecimal price) {
    }

    public record SeatsResult(Long scheduleId, List<SeatInfo> seats) {
    }
}
