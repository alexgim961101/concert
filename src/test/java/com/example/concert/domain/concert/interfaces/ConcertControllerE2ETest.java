package com.example.concert.domain.concert.interfaces;

import com.example.concert.domain.concert.infrastructure.ConcertJpaEntity;
import com.example.concert.domain.concert.infrastructure.ConcertScheduleJpaEntity;
import com.example.concert.domain.concert.infrastructure.SeatJpaEntity;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.infrastructure.QueueTokenJpaEntity;
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
class ConcertControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private ConcertJpaEntity concert;
    private ConcertScheduleJpaEntity schedule1;
    private ConcertScheduleJpaEntity schedule2;
    private String validToken;

    @BeforeEach
    void setUp() {
        concert = new ConcertJpaEntity("IU Concert", "아이유 콘서트");
        entityManager.persist(concert);

        schedule1 = new ConcertScheduleJpaEntity(concert, LocalDateTime.of(2024, 5, 1, 19, 0),
                LocalDateTime.now().minusDays(1));
        schedule2 = new ConcertScheduleJpaEntity(concert, LocalDateTime.of(2024, 5, 2, 19, 0),
                LocalDateTime.now().minusDays(1));
        entityManager.persist(schedule1);
        entityManager.persist(schedule2);

        for (int i = 1; i <= 50; i++) {
            SeatJpaEntity seat = new SeatJpaEntity(schedule1, i, new BigDecimal("100000"));
            entityManager.persist(seat);
        }
        for (int i = 1; i <= 30; i++) {
            SeatJpaEntity seat = new SeatJpaEntity(schedule2, i, new BigDecimal("100000"));
            entityManager.persist(seat);
        }

        validToken = UUID.randomUUID().toString();
        QueueTokenJpaEntity queueToken = new QueueTokenJpaEntity(
                1L, concert.getId(), validToken, TokenStatus.ACTIVE, LocalDateTime.now().plusMinutes(30));
        entityManager.persist(queueToken);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {
        @Test
        @DisplayName("유효한 토큰으로 스케줄 조회 시 200 OK 및 올바른 응답 구조")
        void shouldReturn200_whenValidTokenProvided() throws Exception {
            mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules", concert.getId())
                    .header("Authorization", validToken))
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
    @DisplayName("실패 케이스")
    class FailureCase {
        @Test
        @DisplayName("Authorization 헤더 누락 시 400 Bad Request")
        void shouldReturn400_whenAuthorizationHeaderMissing() throws Exception {
            mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules", concert.getId()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 요청 시 401 Unauthorized")
        void shouldReturn401_whenTokenNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules", concert.getId())
                    .header("Authorization", "non-existent-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("TOKEN_NOT_FOUND"));
        }

        @Test
        @DisplayName("존재하지 않는 콘서트 ID로 요청 시 404 Not Found")
        void shouldReturn404_whenConcertNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules", 999L)
                    .header("Authorization", validToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("CONCERT_NOT_FOUND"));
        }
    }
}
