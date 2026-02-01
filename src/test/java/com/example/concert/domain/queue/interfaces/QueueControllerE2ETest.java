package com.example.concert.domain.queue.interfaces;

import com.example.concert.config.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("QueueController E2E 테스트")
class QueueControllerE2ETest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Nested
        @DisplayName("POST /api/v1/queue/tokens - 토큰 발급")
        class IssueTokenTest {

                @Test
                @DisplayName("토큰 발급 성공 - 첫 번째 요청은 ACTIVE 상태")
                void issueToken_firstRequest_active() throws Exception {
                        // given
                        var request = new QueueController.TokenRequest(1L, 100L);

                        // when & then
                        mockMvc.perform(post("/api/v1/queue/tokens")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.token").isNotEmpty())
                                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                                        .andExpect(jsonPath("$.data.rank").value(0));
                }

                @Test
                @DisplayName("토큰 발급 - 최대 활성 토큰 초과 시 WAITING 상태")
                void issueToken_exceedMaxActive_waiting() throws Exception {
                        // given - 50개의 ACTIVE 토큰 생성
                        for (int i = 1; i <= 50; i++) {
                                var request = new QueueController.TokenRequest((long) i, 200L);
                                mockMvc.perform(post("/api/v1/queue/tokens")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                                .andExpect(status().isOk());
                        }

                        // when - 51번째 토큰 요청
                        var request = new QueueController.TokenRequest(51L, 200L);

                        // then - WAITING 상태
                        mockMvc.perform(post("/api/v1/queue/tokens")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.status").value("WAITING"))
                                        .andExpect(jsonPath("$.data.rank").value(1));
                }

                @Test
                @DisplayName("토큰 발급 실패 - userId 누락")
                void issueToken_missingUserId_badRequest() throws Exception {
                        // given
                        String request = "{\"concertId\": 100}";

                        // when & then
                        mockMvc.perform(post("/api/v1/queue/tokens")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(request))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("토큰 발급 실패 - concertId 누락")
                void issueToken_missingConcertId_badRequest() throws Exception {
                        // given
                        String request = "{\"userId\": 1}";

                        // when & then
                        mockMvc.perform(post("/api/v1/queue/tokens")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(request))
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("GET /api/v1/queue/status - 토큰 상태 조회")
        class GetStatusTest {

                @Test
                @DisplayName("ACTIVE 토큰 상태 조회 성공")
                void getStatus_activeToken_success() throws Exception {
                        // given - 토큰 발급
                        var request = new QueueController.TokenRequest(1L, 300L);
                        MvcResult issueResult = mockMvc.perform(post("/api/v1/queue/tokens")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andReturn();

                        String token = objectMapper.readTree(issueResult.getResponse().getContentAsString())
                                        .path("data").path("token").asText();

                        // when & then
                        mockMvc.perform(get("/api/v1/queue/status")
                                        .header("Concert-Queue-Token", token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.token").value(token))
                                        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
                }

                @Test
                @DisplayName("WAITING 토큰 상태 조회 - 순위 정보 포함")
                void getStatus_waitingToken_includesRank() throws Exception {
                        // given - 51개의 토큰 생성하여 51번째가 WAITING 상태
                        for (int i = 1; i <= 50; i++) {
                                var request = new QueueController.TokenRequest((long) i, 400L);
                                mockMvc.perform(post("/api/v1/queue/tokens")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                                .andExpect(status().isOk());
                        }

                        var request = new QueueController.TokenRequest(51L, 400L);
                        MvcResult issueResult = mockMvc.perform(post("/api/v1/queue/tokens")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andReturn();

                        String token = objectMapper.readTree(issueResult.getResponse().getContentAsString())
                                        .path("data").path("token").asText();

                        // when & then
                        mockMvc.perform(get("/api/v1/queue/status")
                                        .header("Concert-Queue-Token", token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.status").value("WAITING"))
                                        .andExpect(jsonPath("$.data.rank").value(greaterThan(0)));
                }

                @Test
                @DisplayName("존재하지 않는 토큰 조회 - 401 에러")
                void getStatus_invalidToken_unauthorized() throws Exception {
                        // when & then
                        mockMvc.perform(get("/api/v1/queue/status")
                                        .header("Concert-Queue-Token", "invalid-token-uuid"))
                                        .andExpect(status().isUnauthorized());
                }

                @Test
                @DisplayName("토큰 헤더 누락 - 400 에러")
                void getStatus_missingHeader_badRequest() throws Exception {
                        // when & then
                        mockMvc.perform(get("/api/v1/queue/status"))
                                        .andExpect(status().isBadRequest());
                }
        }
}
