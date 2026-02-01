package com.example.concert.domain.concert.infrastructure;

import com.example.concert.config.AbstractIntegrationTest;
import com.example.concert.domain.concert.entity.ConcertSchedule;
import com.example.concert.domain.concert.entity.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ConcertScheduleRepository 통합 테스트")
class ConcertScheduleRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ConcertJpaRepository concertJpaRepository;

    @Autowired
    private ConcertScheduleJpaRepository concertScheduleJpaRepository;

    @Autowired
    private SeatJpaRepository seatJpaRepository;

    @Autowired
    private ConcertScheduleRepositoryImpl concertScheduleRepository;

    @Autowired
    private SeatRepositoryImpl seatRepository;

    private ConcertJpaEntity concert;
    private ConcertScheduleJpaEntity schedule1;
    private ConcertScheduleJpaEntity schedule2;

    @BeforeEach
    void setUp() {
        // Clean up previous data
        seatJpaRepository.deleteAll();
        concertScheduleJpaRepository.deleteAll();
        concertJpaRepository.deleteAll();

        concert = new ConcertJpaEntity("Test Concert", "Test Description");
        concertJpaRepository.saveAndFlush(concert);

        schedule1 = new ConcertScheduleJpaEntity(concert, LocalDateTime.of(2024, 5, 1, 19, 0),
                LocalDateTime.now().minusDays(1));
        schedule2 = new ConcertScheduleJpaEntity(concert, LocalDateTime.of(2024, 5, 2, 19, 0),
                LocalDateTime.now().minusDays(1));
        concertScheduleJpaRepository.saveAndFlush(schedule1);
        concertScheduleJpaRepository.saveAndFlush(schedule2);

        for (int i = 1; i <= 3; i++) {
            SeatJpaEntity seat = new SeatJpaEntity(schedule1, i, new BigDecimal("10000"));
            seatJpaRepository.saveAndFlush(seat);
        }
        SeatJpaEntity reservedSeat1 = new SeatJpaEntity(schedule1, 4, new BigDecimal("10000"));
        reservedSeat1.setStatus(SeatStatus.RESERVED);
        seatJpaRepository.saveAndFlush(reservedSeat1);
        SeatJpaEntity reservedSeat2 = new SeatJpaEntity(schedule1, 5, new BigDecimal("10000"));
        reservedSeat2.setStatus(SeatStatus.RESERVED);
        seatJpaRepository.saveAndFlush(reservedSeat2);

        for (int i = 1; i <= 2; i++) {
            SeatJpaEntity seat = new SeatJpaEntity(schedule2, i, new BigDecimal("15000"));
            seatJpaRepository.saveAndFlush(seat);
        }
    }

    @Test
    @DisplayName("콘서트 ID로 스케줄 목록 조회")
    void shouldFindSchedulesByConcertId() {
        List<ConcertSchedule> schedules = concertScheduleRepository.findByConcertId(concert.getId());
        assertThat(schedules).hasSize(2);
    }

    @Test
    @DisplayName("콘서트 존재 여부 확인 - 존재하는 경우")
    void shouldReturnTrue_whenConcertExists() {
        boolean exists = concertScheduleRepository.existsConcertById(concert.getId());
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("콘서트 존재 여부 확인 - 존재하지 않는 경우")
    void shouldReturnFalse_whenConcertNotExists() {
        boolean exists = concertScheduleRepository.existsConcertById(999L);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("스케줄별 가용 좌석 수 조회")
    void shouldCountAvailableSeats() {
        int schedule1Available = seatRepository.countByScheduleIdAndStatus(schedule1.getId(), SeatStatus.AVAILABLE);
        int schedule2Available = seatRepository.countByScheduleIdAndStatus(schedule2.getId(), SeatStatus.AVAILABLE);
        assertThat(schedule1Available).isEqualTo(3);
        assertThat(schedule2Available).isEqualTo(2);
    }

    @Test
    @DisplayName("RESERVED 상태 좌석 수 조회")
    void shouldCountReservedSeats() {
        int reserved = seatRepository.countByScheduleIdAndStatus(schedule1.getId(), SeatStatus.RESERVED);
        assertThat(reserved).isEqualTo(2);
    }
}
