package com.example.concert.domain.concert.infrastructure;

import com.example.concert.domain.concert.entity.Seat;
import com.example.concert.domain.concert.entity.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(SeatRepositoryImpl.class)
@DisplayName("SeatRepository 통합 테스트")
class SeatRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SeatRepositoryImpl seatRepository;

    private ConcertJpaEntity concert;
    private ConcertScheduleJpaEntity schedule;
    private Long scheduleId;

    @BeforeEach
    void setUp() {
        // Concert 생성
        concert = new ConcertJpaEntity("테스트 콘서트", "테스트 설명");
        entityManager.persist(concert);

        // Schedule 생성
        schedule = new ConcertScheduleJpaEntity(
                concert,
                LocalDateTime.now().plusDays(30),
                LocalDateTime.now().minusDays(1));
        entityManager.persist(schedule);
        scheduleId = schedule.getId();

        // Seats 생성 (AVAILABLE 3개, RESERVED 2개)
        for (int i = 1; i <= 3; i++) {
            SeatJpaEntity seat = new SeatJpaEntity(schedule, i, BigDecimal.valueOf(10000));
            entityManager.persist(seat);
        }
        for (int i = 4; i <= 5; i++) {
            SeatJpaEntity seat = new SeatJpaEntity(schedule, i, BigDecimal.valueOf(10000));
            seat.setStatus(SeatStatus.RESERVED);
            entityManager.persist(seat);
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findAllByScheduleId 테스트")
    class FindAllByScheduleIdTest {

        @Test
        @DisplayName("스케줄 ID로 전체 좌석 조회 시 모든 좌석 반환")
        void shouldReturnAllSeats() {
            // when
            List<Seat> seats = seatRepository.findAllByScheduleId(scheduleId);

            // then
            assertThat(seats).hasSize(5);
        }

        @Test
        @DisplayName("존재하지 않는 스케줄 ID로 조회 시 빈 리스트 반환")
        void shouldReturnEmptyList_WhenScheduleNotExists() {
            // when
            List<Seat> seats = seatRepository.findAllByScheduleId(9999L);

            // then
            assertThat(seats).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByScheduleIdAndStatusIn 테스트")
    class FindAllByScheduleIdAndStatusInTest {

        @Test
        @DisplayName("AVAILABLE 상태로 필터링 시 해당 좌석만 반환")
        void shouldReturnOnlyAvailableSeats() {
            // when
            List<Seat> seats = seatRepository.findAllByScheduleIdAndStatusIn(
                    scheduleId, List.of(SeatStatus.AVAILABLE));

            // then
            assertThat(seats).hasSize(3);
            assertThat(seats).allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE);
        }

        @Test
        @DisplayName("RESERVED 상태로 필터링 시 해당 좌석만 반환")
        void shouldReturnOnlyReservedSeats() {
            // when
            List<Seat> seats = seatRepository.findAllByScheduleIdAndStatusIn(
                    scheduleId, List.of(SeatStatus.RESERVED));

            // then
            assertThat(seats).hasSize(2);
            assertThat(seats).allMatch(s -> s.getStatus() == SeatStatus.RESERVED);
        }

        @Test
        @DisplayName("복수 상태로 필터링 시 해당 상태 모두 반환")
        void shouldReturnMultipleStatusSeats() {
            // when
            List<Seat> seats = seatRepository.findAllByScheduleIdAndStatusIn(
                    scheduleId, List.of(SeatStatus.AVAILABLE, SeatStatus.RESERVED));

            // then
            assertThat(seats).hasSize(5);
        }
    }
}
