package com.example.concert.domain.concert.service;

import com.example.concert.domain.concert.entity.ConcertSchedule;
import com.example.concert.domain.concert.entity.Seat;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.repository.ConcertScheduleRepository;
import com.example.concert.domain.concert.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 콘서트 조회 서비스 (캐싱 적용)
 * UseCase에서 분리된 순수 조회 로직을 담당하며, 캐싱을 통해 성능을 최적화합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertScheduleRepository concertScheduleRepository;
    private final SeatRepository seatRepository;

    /**
     * 콘서트 스케줄 및 가용 좌석 수 조회 (캐시 적용)
     * - sync = true: 동일 키에 대해 한 스레드만 DB 조회 (Cache Stampede 방지)
     */
    @Cacheable(value = "concertSchedules", key = "#concertId", sync = true)
    public SchedulesWithSeatsResult getSchedulesWithSeats(Long concertId) {
        log.debug("Cache miss - fetching schedules from DB for concertId: {}", concertId);

        List<ConcertSchedule> schedules = concertScheduleRepository.findByConcertId(concertId);

        List<ScheduleInfo> scheduleInfos = schedules.stream()
                .map(schedule -> new ScheduleInfo(
                        schedule.getId(),
                        schedule.getConcertDate().toLocalDate(),
                        seatRepository.countByScheduleIdAndStatus(schedule.getId(), SeatStatus.AVAILABLE)))
                .toList();

        return new SchedulesWithSeatsResult(concertId, scheduleInfos);
    }

    /**
     * 좌석 목록 조회 (캐시 적용)
     * - 상태 필터 없이 전체 조회 시에만 캐싱 (필터 있으면 캐시 키가 복잡해지므로)
     */
    @Cacheable(value = "seats", key = "#scheduleId", sync = true, condition = "#statuses == null || #statuses.isEmpty()")
    public SeatsResult getSeats(Long scheduleId, List<SeatStatus> statuses) {
        log.debug("Cache miss - fetching seats from DB for scheduleId: {}", scheduleId);

        List<Seat> seats;
        if (statuses == null || statuses.isEmpty()) {
            seats = seatRepository.findAllByScheduleId(scheduleId);
        } else {
            seats = seatRepository.findAllByScheduleIdAndStatusIn(scheduleId, statuses);
        }

        List<SeatInfo> seatInfos = seats.stream()
                .map(seat -> new SeatInfo(
                        seat.getId(),
                        seat.getSeatNumber(),
                        seat.getStatus(),
                        seat.getPrice()))
                .toList();

        return new SeatsResult(scheduleId, seatInfos);
    }

    /**
     * 좌석 캐시 갱신 (Write-Through)
     * - 예약 성공 후 호출하여 캐시를 최신 상태로 갱신
     * - Cache Eviction 대신 사용하여 Stampede 방지
     */
    @CachePut(value = "seats", key = "#scheduleId")
    public SeatsResult refreshSeatsCache(Long scheduleId) {
        log.debug("Refreshing seats cache for scheduleId: {}", scheduleId);

        List<Seat> seats = seatRepository.findAllByScheduleId(scheduleId);

        List<SeatInfo> seatInfos = seats.stream()
                .map(seat -> new SeatInfo(
                        seat.getId(),
                        seat.getSeatNumber(),
                        seat.getStatus(),
                        seat.getPrice()))
                .toList();

        return new SeatsResult(scheduleId, seatInfos);
    }

    // DTO Records (Serializable for Redis JDK serialization)
    public record ScheduleInfo(Long id, LocalDate date, int availableSeats) implements Serializable {
    }

    public record SchedulesWithSeatsResult(Long concertId, List<ScheduleInfo> schedules) implements Serializable {
    }

    public record SeatInfo(Long id, Integer number, SeatStatus status, BigDecimal price) implements Serializable {
    }

    public record SeatsResult(Long scheduleId, List<SeatInfo> seats) implements Serializable {
    }
}
