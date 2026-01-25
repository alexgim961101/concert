package com.example.concert.common.config;

import com.example.concert.domain.concert.repository.ConcertScheduleRepository;
import com.example.concert.domain.concert.service.ConcertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플리케이션 시작 시 임박 콘서트의 캐시를 미리 적재 (Pre-warming)
 * - 첫 요청 시 Cold Start로 인한 지연을 방지
 * - 기본적으로 오늘~7일 내 공연이 있는 콘서트만 웜업
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupRunner implements ApplicationRunner {

    private static final int WARMUP_DAYS_AHEAD = 7;

    private final ConcertScheduleRepository scheduleRepository;
    private final ConcertService concertService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Starting cache warm-up for upcoming concerts (next {} days)...", WARMUP_DAYS_AHEAD);

            List<Long> upcomingConcertIds = scheduleRepository.findUpcomingConcertIds(WARMUP_DAYS_AHEAD);

            if (upcomingConcertIds.isEmpty()) {
                log.info("No upcoming concerts found for warm-up.");
                return;
            }

            int successCount = 0;
            for (Long concertId : upcomingConcertIds) {
                try {
                    concertService.getSchedulesWithSeats(concertId);
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to warm-up cache for concertId: {}", concertId, e);
                }
            }

            log.info("Cache warm-up completed: {}/{} concerts cached successfully.",
                    successCount, upcomingConcertIds.size());
        } catch (Exception e) {
            log.error("Cache warm-up failed", e);
            // 웜업 실패는 애플리케이션 시작에 영향을 주지 않도록 예외를 삼킴
        }
    }
}
