package com.example.concert.domain.reservation.infrastructure;

import com.example.concert.domain.reservation.usecase.ExpireReservationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 1분마다 만료된 예약을 처리하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {
    private final ExpireReservationUseCase expireReservationUseCase;

    @Scheduled(fixedDelay = 60000)
    public void expireReservations() {
        log.debug("Running reservation expiration scheduler");
        int processed = expireReservationUseCase.execute();
        if (processed > 0) {
            log.info("Reservation expiration scheduler completed: {} reservations expired", processed);
        }
    }
}
