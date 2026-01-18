package com.example.concert.domain.reservation.interfaces;

import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.infrastructure.*;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.infrastructure.QueueTokenJpaEntity;
import com.example.concert.domain.queue.infrastructure.QueueTokenJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ReservationController E2E 테스트")
class ReservationControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private ConcertScheduleJpaEntity schedule;
    private SeatJpaEntity availableSeat;
    private SeatJpaEntity reservedSeat;
    private String validToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;

        concert = new ConcertJpaEntity("IU Concert", "아이유 콘서트");
        concertJpaRepository.save(concert);

        schedule = new ConcertScheduleJpaEntity(concert, LocalDateTime.of(2024, 5, 1, 19, 0),
                LocalDateTime.now().minusDays(1));
        scheduleJpaRepository.save(schedule);

        // AVAILABLE seat
        availableSeat = new SeatJpaEntity(schedule, 1, new BigDecimal("100000"));
        seatJpaRepository.save(availableSeat);

        // Already RESERVED seat
        reservedSeat = new SeatJpaEntity(schedule, 2, new BigDecimal("100000"));
        reservedSeat.setStatus(SeatStatus.RESERVED);
        seatJpaRepository.save(reservedSeat);

        validToken = UUID.randomUUID().toString();
        QueueTokenJpaEntity queueToken = new QueueTokenJpaEntity(
                userId, concert.getId(), validToken, TokenStatus.ACTIVE, LocalDateTime.now().plusMinutes(30));
        queueTokenJpaRepository.save(queueToken);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("예약 성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("유효한 요청으로 좌석 예약 시 200 OK 및 PENDING 상태 반환")
        void shouldReturn200_whenValidRequest() throws Exception {
            ReservationController.ReservationRequest request = new ReservationController.ReservationRequest(
                    userId, schedule.getId(), availableSeat.getId());

            mockMvc.perform(post("/api/v1/reservations")
                    .header("Concert-Queue-Token", validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reservationId").exists())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.expiresAt").exists());
        }
    }

    @Nested
    @DisplayName("예약 실패 케이스")
    class FailureCases {

        @Test
        @DisplayName("이미 예약된 좌석 요청 시 409 Conflict")
        void shouldReturn409_whenSeatAlreadyReserved() throws Exception {
            ReservationController.ReservationRequest request = new ReservationController.ReservationRequest(
                    userId, schedule.getId(), reservedSeat.getId());

            mockMvc.perform(post("/api/v1/reservations")
                    .header("Concert-Queue-Token", validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("SEAT_NOT_AVAILABLE"));
        }

        @Test
        @DisplayName("존재하지 않는 좌석 요청 시 404 Not Found")
        void shouldReturn404_whenSeatNotFound() throws Exception {
            ReservationController.ReservationRequest request = new ReservationController.ReservationRequest(
                    userId, schedule.getId(), 99999L);

            mockMvc.perform(post("/api/v1/reservations")
                    .header("Concert-Queue-Token", validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("SEAT_NOT_FOUND"));
        }

        @Test
        @DisplayName("Concert-Queue-Token 헤더 누락 시 400 Bad Request")
        void shouldReturn400_whenTokenMissing() throws Exception {
            ReservationController.ReservationRequest request = new ReservationController.ReservationRequest(
                    userId, schedule.getId(), availableSeat.getId());

            mockMvc.perform(post("/api/v1/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 요청 시 401 Unauthorized")
        void shouldReturn401_whenTokenInvalid() throws Exception {
            ReservationController.ReservationRequest request = new ReservationController.ReservationRequest(
                    userId, schedule.getId(), availableSeat.getId());

            mockMvc.perform(post("/api/v1/reservations")
                    .header("Concert-Queue-Token", "invalid-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("TOKEN_NOT_FOUND"));
        }

        @Test
        @DisplayName("존재하지 않는 스케줄 요청 시 404 Not Found")
        void shouldReturn404_whenScheduleNotFound() throws Exception {
            ReservationController.ReservationRequest request = new ReservationController.ReservationRequest(
                    userId, 99999L, availableSeat.getId());

            mockMvc.perform(post("/api/v1/reservations")
                    .header("Concert-Queue-Token", validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 Bad Request")
        void shouldReturn400_whenRequiredFieldMissing() throws Exception {
            String invalidRequest = "{\"scheduleId\": 1, \"seatId\": 1}"; // userId 누락

            mockMvc.perform(post("/api/v1/reservations")
                    .header("Concert-Queue-Token", validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }
}
