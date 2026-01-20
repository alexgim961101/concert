package com.example.concert.domain.point.interfaces;

import com.example.concert.domain.point.infrastructure.PointJpaEntity;
import com.example.concert.domain.point.infrastructure.PointJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PointController E2E 테스트")
class PointControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        pointJpaRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("PATCH /api/v1/points/charge")
    class ChargePoint {

        @Test
        @DisplayName("정상 충전 요청 -> 200 OK, 잔액 증가")
        void shouldChargePointSuccessfully() throws Exception {
            PointController.ChargeRequest request = new PointController.ChargeRequest(userId, new BigDecimal("10000"));

            mockMvc.perform(patch("/api/v1/points/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.currentBalance").value(10000));

            // DB 검증
            PointJpaEntity saved = pointJpaRepository.findByUserId(userId).orElseThrow();
            assertThat(saved.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("기존 포인트에 추가 충전 -> 잔액 누적")
        void shouldAccumulatePoint() throws Exception {
            // Given: 기존 5000원
            PointJpaEntity existing = new PointJpaEntity(userId, new BigDecimal("5000"));
            pointJpaRepository.save(existing);
            entityManager.flush();
            entityManager.clear();

            PointController.ChargeRequest request = new PointController.ChargeRequest(userId, new BigDecimal("3000"));

            mockMvc.perform(patch("/api/v1/points/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentBalance").value(8000));
        }

        @Test
        @DisplayName("0원 충전 시도 -> 400 Bad Request")
        void shouldReturn400_whenAmountIsZero() throws Exception {
            PointController.ChargeRequest request = new PointController.ChargeRequest(userId, BigDecimal.ZERO);

            mockMvc.perform(patch("/api/v1/points/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("음수 충전 시도 -> 400 Bad Request")
        void shouldReturn400_whenAmountIsNegative() throws Exception {
            PointController.ChargeRequest request = new PointController.ChargeRequest(userId, new BigDecimal("-1000"));

            mockMvc.perform(patch("/api/v1/points/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("최소 충전 금액 미달 시 -> 400 Bad Request")
        void shouldReturn400_whenAmountBelowMinimum() throws Exception {
            PointController.ChargeRequest request = new PointController.ChargeRequest(userId, new BigDecimal("50"));

            mockMvc.perform(patch("/api/v1/points/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("최대 한도 초과 시 -> 400 Bad Request")
        void shouldReturn400_whenExceedingMaxLimit() throws Exception {
            // Given: 기존 999,000원
            PointJpaEntity existing = new PointJpaEntity(userId, new BigDecimal("999000"));
            pointJpaRepository.save(existing);
            entityManager.flush();
            entityManager.clear();

            // 2,000원 충전 시도 (합계 1,001,000원 -> 최대 한도 1,000,000원 초과)
            PointController.ChargeRequest request = new PointController.ChargeRequest(userId, new BigDecimal("2000"));

            mockMvc.perform(patch("/api/v1/points/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("userId 누락 시 -> 400 Bad Request")
        void shouldReturn400_whenUserIdMissing() throws Exception {
            String invalidRequest = "{\"amount\": 10000}";

            mockMvc.perform(patch("/api/v1/points/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("amount 누락 시 -> 400 Bad Request")
        void shouldReturn400_whenAmountMissing() throws Exception {
            String invalidRequest = "{\"userId\": 1}";

            mockMvc.perform(patch("/api/v1/points/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/points/{userId}")
    class GetPoint {

        @Test
        @DisplayName("포인트가 존재하는 유저 조회 -> 200 OK, 잔액 반환")
        void shouldReturnPointSuccessfully() throws Exception {
            // Given: 5000원 보유
            PointJpaEntity existing = new PointJpaEntity(userId, new BigDecimal("5000"));
            pointJpaRepository.save(existing);
            entityManager.flush();
            entityManager.clear();

            // When & Then
            mockMvc.perform(get("/api/v1/points/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.point").value(5000));
        }

        @Test
        @DisplayName("포인트 기록 없는 유저 조회 -> 200 OK, 잔액 0원 반환")
        void shouldReturnZeroPointForNewUser() throws Exception {
            // Given: DB에 데이터 없음
            Long newUserId = 999L;

            // When & Then
            mockMvc.perform(get("/api/v1/points/{userId}", newUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(newUserId))
                    .andExpect(jsonPath("$.data.point").value(0));
        }
    }
}
