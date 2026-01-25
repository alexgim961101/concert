package com.example.concert.domain.concert.interfaces;

import com.example.concert.config.AbstractIntegrationTest;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.infrastructure.*;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.infrastructure.QueueTokenJpaEntity;
import com.example.concert.domain.queue.infrastructure.QueueTokenJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ConcertController E2E 테스트")
class ConcertControllerE2ETest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ConcertJpaRepository concertJpaRepository;

    @Autowired
    private ConcertScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private SeatJpaRepository seatJpaRepository;

    @Autowired
    private QueueTokenJpaRepository queueTokenJpaRepository;

    private ConcertJpaEntity concert;
    private ConcertScheduleJpaEntity schedule1;
    private ConcertScheduleJpaEntity schedule2;
    private Long scheduleId;
    private String validToken;

    @BeforeEach
    void setUp() {
        concert = new ConcertJpaEntity("IU Concert", "아이유 콘서트");
        concertJpaRepository.save(concert);

        schedule1 = new ConcertScheduleJpaEntity(concert, LocalDateTime.of(2024, 5, 1, 19, 0),
                LocalDateTime.now().minusDays(1));
        schedule2 = new ConcertScheduleJpaEntity(concert, LocalDateTime.of(2024, 5, 2, 19, 0),
                LocalDateTime.now().minusDays(1));
        scheduleJpaRepository.save(schedule1);
        scheduleJpaRepository.save(schedule2);
        scheduleId = schedule1.getId();

        // Schedule1: 50 AVAILABLE seats
        for (int i = 1; i <= 50; i++) {
            SeatJpaEntity seat = new SeatJpaEntity(schedule1, i, new BigDecimal("100000"));
            seatJpaRepository.save(seat);
        }
        // Schedule2: 30 AVAILABLE seats
        for (int i = 1; i <= 30; i++) {
            SeatJpaEntity seat = new SeatJpaEntity(schedule2, i, new BigDecimal("100000"));
            seatJpaRepository.save(seat);
        }

        // 좌석 조회 테스트용: schedule1에 RESERVED 좌석 1개 추가
        SeatJpaEntity reservedSeat = new SeatJpaEntity(schedule1, 51, new BigDecimal("150000"));
        reservedSeat.setStatus(SeatStatus.RESERVED);
        seatJpaRepository.save(reservedSeat);

        validToken = UUID.randomUUID().toString();
        QueueTokenJpaEntity queueToken = new QueueTokenJpaEntity(
                1L, concert.getId(), validToken, TokenStatus.ACTIVE, LocalDateTime.now().plusMinutes(30));
        queueTokenJpaRepository.save(queueToken);

        entityManager.flush();
        entityManager.clear();
    }

    // ===== 스케줄 조회 테스트 =====
    @Nested
    @DisplayName("스케줄 조회 - 성공 케이스")
    class ScheduleSuccessCase {
        @Test
        @DisplayName("유효한 토큰으로 스케줄 조회 시 200 OK 및 올바른 응답 구조")
        void shouldReturn200_whenValidTokenProvided() throws Exception {
            mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules", concert.getId())
                    .header("Concert-Queue-Token", validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.concertId").value(concert.getId()))
                    .andExpect(jsonPath("$.schedules").isArray())
                    .andExpect(jsonPath("$.schedules", hasSize(2)))
                    .andExpect(jsonPath("$.schedules[0].id").exists())
                    .andExpect(jsonPath("$.schedules[0].date").exists())
                    .andExpect(jsonPath("$.schedules[0].availableSeats").value(50))
                    .andExpect(jsonPath("$.schedules[1].availableSeats").value(30));
        }
    }

    @Nested
    @DisplayName("스케줄 조회 - 실패 케이스")
    class ScheduleFailureCase {
        @Test
        @DisplayName("Concert-Queue-Token 헤더 누락 시 400 Bad Request")
        void shouldReturn400_whenConcertQueueTokenHeaderMissing() throws Exception {
            mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules", concert.getId()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 요청 시 401 Unauthorized")
        void shouldReturn401_whenTokenNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules", concert.getId())
                    .header("Concert-Queue-Token", "non-existent-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("TOKEN_NOT_FOUND"));
        }

        @Test
        @DisplayName("존재하지 않는 콘서트 ID로 요청 시 404 Not Found")
        void shouldReturn404_whenConcertNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules", 999L)
                    .header("Concert-Queue-Token", validToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("CONCERT_NOT_FOUND"));
        }
    }

    // ===== 좌석 조회 테스트 =====
    @Nested
    @DisplayName("좌석 조회 - 성공 케이스")
    class SeatsSuccessCases {

        @Test
        @DisplayName("전체 좌석 조회 - status 파라미터 없음")
        void shouldReturnAllSeats_WhenNoStatusParam() throws Exception {
            mockMvc.perform(get("/api/v1/schedules/{scheduleId}/seats", scheduleId)
                    .header("Concert-Queue-Token", validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                    .andExpect(jsonPath("$.seats", hasSize(51))) // 50 AVAILABLE + 1 RESERVED
                    .andExpect(jsonPath("$.seats[*].id").exists())
                    .andExpect(jsonPath("$.seats[*].number").exists())
                    .andExpect(jsonPath("$.seats[*].status").exists())
                    .andExpect(jsonPath("$.seats[*].price").exists());
        }

        @Test
        @DisplayName("AVAILABLE 상태 좌석만 조회")
        void shouldReturnOnlyAvailableSeats_WhenStatusParamProvided() throws Exception {
            mockMvc.perform(get("/api/v1/schedules/{scheduleId}/seats", scheduleId)
                    .param("status", "AVAILABLE")
                    .header("Concert-Queue-Token", validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seats", hasSize(50)))
                    .andExpect(jsonPath("$.seats[*].status", everyItem(is("AVAILABLE"))));
        }

        @Test
        @DisplayName("복수 상태 필터링")
        void shouldReturnMultipleStatusSeats() throws Exception {
            mockMvc.perform(get("/api/v1/schedules/{scheduleId}/seats", scheduleId)
                    .param("status", "AVAILABLE", "RESERVED")
                    .header("Concert-Queue-Token", validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seats", hasSize(51)));
        }
    }

    @Nested
    @DisplayName("좌석 조회 - 실패 케이스")
    class SeatsFailureCases {

        @Test
        @DisplayName("Concert-Queue-Token 헤더 누락 시 400 Bad Request")
        void shouldReturn400_WhenConcertQueueTokenHeaderMissing() throws Exception {
            mockMvc.perform(get("/api/v1/schedules/{scheduleId}/seats", scheduleId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 요청 시 401 Unauthorized")
        void shouldReturn401_WhenTokenNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/schedules/{scheduleId}/seats", scheduleId)
                    .header("Concert-Queue-Token", "invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
