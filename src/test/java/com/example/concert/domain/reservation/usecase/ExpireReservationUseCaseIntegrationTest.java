package com.example.concert.domain.reservation.usecase;

import com.example.concert.config.AbstractIntegrationTest;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.infrastructure.ConcertJpaEntity;
import com.example.concert.domain.concert.infrastructure.ConcertJpaRepository;
import com.example.concert.domain.concert.infrastructure.ConcertScheduleJpaEntity;
import com.example.concert.domain.concert.infrastructure.ConcertScheduleJpaRepository;
import com.example.concert.domain.concert.infrastructure.SeatJpaEntity;
import com.example.concert.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert.domain.reservation.entity.ReservationStatus;
import com.example.concert.domain.reservation.infrastructure.ReservationJpaEntity;
import com.example.concert.domain.reservation.infrastructure.ReservationJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ExpireReservationUseCase 통합 테스트")
class ExpireReservationUseCaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpireReservationUseCase expireReservationUseCase;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private SeatJpaRepository seatJpaRepository;

    @Autowired
    private ConcertJpaRepository concertJpaRepository;

    @Autowired
    private ConcertScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private ConcertJpaEntity concert;
    private ConcertScheduleJpaEntity schedule;
    private SeatJpaEntity expiredSeat;
    private SeatJpaEntity validSeat;
    private ReservationJpaEntity expiredReservation;
    private ReservationJpaEntity validReservation;

    @BeforeEach
    void setUp() {
        concert = new ConcertJpaEntity("Test Concert", "Description");
        concertJpaRepository.save(concert);

        schedule = new ConcertScheduleJpaEntity(concert, LocalDateTime.now().plusDays(7),
                LocalDateTime.now().minusDays(1));
        scheduleJpaRepository.save(schedule);

        // 만료된 예약의 좌석 (TEMP_RESERVED 상태)
        expiredSeat = new SeatJpaEntity(schedule, 1, new BigDecimal("100000"));
        expiredSeat.setStatus(SeatStatus.TEMP_RESERVED);
        seatJpaRepository.save(expiredSeat);

        // 만료되지 않은 예약의 좌석
        validSeat = new SeatJpaEntity(schedule, 2, new BigDecimal("100000"));
        validSeat.setStatus(SeatStatus.TEMP_RESERVED);
        seatJpaRepository.save(validSeat);

        // 만료된 예약 (5분 전에 만료됨)
        expiredReservation = new ReservationJpaEntity(
                1L, schedule.getId(), expiredSeat.getId(),
                ReservationStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        reservationJpaRepository.save(expiredReservation);

        // 유효한 예약 (5분 후에 만료됨)
        validReservation = new ReservationJpaEntity(
                1L, schedule.getId(), validSeat.getId(),
                ReservationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        reservationJpaRepository.save(validReservation);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("만료된 예약이 EXPIRED 처리되고 좌석이 AVAILABLE로 변경된다")
    void shouldExpireReservationAndReleaseSeat() {
        // when
        int processedCount = expireReservationUseCase.execute();

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(processedCount).isEqualTo(1);

        // 만료된 예약 확인
        ReservationJpaEntity expired = reservationJpaRepository.findById(expiredReservation.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        // 만료된 좌석 확인
        SeatJpaEntity releasedSeat = seatJpaRepository.findById(expiredSeat.getId()).orElseThrow();
        assertThat(releasedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("유효한 예약은 그대로 유지된다")
    void shouldNotExpireValidReservation() {
        // when
        expireReservationUseCase.execute();

        entityManager.flush();
        entityManager.clear();

        // then
        ReservationJpaEntity valid = reservationJpaRepository.findById(validReservation.getId()).orElseThrow();
        assertThat(valid.getStatus()).isEqualTo(ReservationStatus.PENDING);

        SeatJpaEntity seat = seatJpaRepository.findById(validSeat.getId()).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.TEMP_RESERVED);
    }

    @Test
    @DisplayName("만료된 예약이 없으면 0을 반환한다")
    void shouldReturnZeroWhenNoExpiredReservations() {
        // given - 만료된 예약 삭제
        reservationJpaRepository.delete(expiredReservation);
        entityManager.flush();
        entityManager.clear();

        // when
        int processedCount = expireReservationUseCase.execute();

        // then
        assertThat(processedCount).isEqualTo(0);
    }
}
