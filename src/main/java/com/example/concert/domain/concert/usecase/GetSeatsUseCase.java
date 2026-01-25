package com.example.concert.domain.concert.usecase;

import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.service.ConcertService;
import com.example.concert.domain.queue.usecase.ValidateTokenUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSeatsUseCase {

    private final ValidateTokenUseCase validateTokenUseCase;
    private final ConcertService concertService;

    public SeatsResult execute(String token, Long scheduleId, List<SeatStatus> statuses) {
        // 1. 토큰 검증 (매번 수행)
        validateTokenUseCase.execute(token);

        // 2. 좌석 조회 (캐시 적용됨 - 상태 필터 없을 때만)
        ConcertService.SeatsResult cachedResult = concertService.getSeats(scheduleId, statuses);

        // 3. ConcertService DTO → UseCase DTO 변환
        List<SeatInfo> seatInfos = cachedResult.seats().stream()
                .map(s -> new SeatInfo(s.id(), s.number(), s.status(), s.price()))
                .toList();

        return new SeatsResult(scheduleId, seatInfos);
    }

    public record SeatInfo(Long id, Integer number, SeatStatus status, BigDecimal price) {
    }

    public record SeatsResult(Long scheduleId, List<SeatInfo> seats) {
    }
}
