package com.example.concert.domain.concert.usecase;

import com.example.concert.domain.concert.repository.ConcertScheduleRepository;
import com.example.concert.domain.concert.service.ConcertService;
import com.example.concert.domain.concert.service.ConcertService.ScheduleInfo;
import com.example.concert.domain.concert.service.ConcertService.SchedulesWithSeatsResult;
import com.example.concert.domain.queue.usecase.ValidateTokenUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAvailableDatesUseCase {
    private final ValidateTokenUseCase validateTokenUseCase;
    private final ConcertScheduleRepository concertScheduleRepository;
    private final ConcertService concertService;

    public AvailableDatesResult execute(String token, Long concertId) {
        // 1. 토큰 검증 (매번 수행, 캐싱하지 않음)
        validateTokenUseCase.execute(token);

        // 2. 콘서트 존재 여부 확인
        if (!concertScheduleRepository.existsConcertById(concertId)) {
            throw new ConcertNotFoundException(concertId);
        }

        // 3. 스케줄 및 가용 좌석 수 조회 (캐시 적용됨)
        SchedulesWithSeatsResult cachedResult = concertService.getSchedulesWithSeats(concertId);

        // 4. ConcertService DTO → UseCase DTO 변환
        List<ScheduleInfo> scheduleInfos = cachedResult.schedules().stream()
                .map(s -> new ScheduleInfo(s.id(), s.date(), s.availableSeats()))
                .toList();

        return new AvailableDatesResult(concertId, scheduleInfos);
    }

    public record ScheduleInfo(Long id, LocalDate date, int availableSeats) {
    }

    public record AvailableDatesResult(Long concertId, List<ScheduleInfo> schedules) {
    }
}
