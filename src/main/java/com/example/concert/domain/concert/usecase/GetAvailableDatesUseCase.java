package com.example.concert.domain.concert.usecase;

import com.example.concert.domain.concert.entity.ConcertSchedule;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.repository.ConcertScheduleRepository;
import com.example.concert.domain.concert.repository.SeatRepository;
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
    private final SeatRepository seatRepository;

    public AvailableDatesResult execute(String token, Long concertId) {
        // 1. 토큰 검증
        validateTokenUseCase.execute(token);

        // 2. 콘서트 존재 여부 확인
        if (!concertScheduleRepository.existsConcertById(concertId)) {
            throw new ConcertNotFoundException(concertId);
        }

        // 3. 스케줄 조회
        List<ConcertSchedule> schedules = concertScheduleRepository.findByConcertId(concertId);

        // 4. 가용 좌석 수 계산
        List<ScheduleInfo> scheduleInfos = schedules.stream()
                .map(schedule -> new ScheduleInfo(
                        schedule.getId(),
                        schedule.getConcertDate().toLocalDate(),
                        seatRepository.countByScheduleIdAndStatus(schedule.getId(), SeatStatus.AVAILABLE)))
                .toList();

        return new AvailableDatesResult(concertId, scheduleInfos);
    }

    public record ScheduleInfo(Long id, LocalDate date, int availableSeats) {
    }

    public record AvailableDatesResult(Long concertId, List<ScheduleInfo> schedules) {
    }
}
