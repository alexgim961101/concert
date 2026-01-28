package com.example.concert.domain.payment.interfaces;

import com.example.concert.config.AbstractIntegrationTest;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.infrastructure.*;
import com.example.concert.domain.point.infrastructure.PointJpaEntity;
import com.example.concert.domain.point.infrastructure.PointJpaRepository;
import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.infrastructure.RedisQueueTokenRepositoryImpl;
import com.example.concert.domain.reservation.entity.ReservationStatus;
import com.example.concert.domain.reservation.infrastructure.ReservationJpaEntity;
import com.example.concert.domain.reservation.infrastructure.ReservationJpaRepository;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PaymentController E2E 테스트")
class PaymentControllerE2ETest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConcertJpaRepository concertJpaRepository;

    @Autowired
    private ConcertScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private SeatJpaRepository seatJpaRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private RedisQueueTokenRepositoryImpl queueTokenRepository;

    @Autowired
    private EntityManager entityManager;

    private Long userId;
    private Long seatId;
    private Long reservationId;
    private String token;

    @BeforeEach
    void setUp() {
        userId = 1L;

        // Concert 생성
        ConcertJpaEntity concert = new ConcertJpaEntity("Test Concert", "Description");
        concert = concertJpaRepository.save(concert);

        // Schedule 생성
        ConcertScheduleJpaEntity schedule = new ConcertScheduleJpaEntity(
                concert, LocalDateTime.now().plusDays(7), LocalDateTime.now().minusDays(1));
        schedule = scheduleJpaRepository.save(schedule);

        // 좌석 생성 (TEMP_RESERVED, 10000원)
        SeatJpaEntity seat = new SeatJpaEntity(schedule, 1, new BigDecimal("10000"));
        seat.setStatus(SeatStatus.TEMP_RESERVED);
        seat = seatJpaRepository.save(seat);
        seatId = seat.getId();

        // 예약 생성 (PENDING, 5분 후 만료)
        ReservationJpaEntity reservation = new ReservationJpaEntity(
                userId, schedule.getId(), seatId, ReservationStatus.PENDING,
                LocalDateTime.now().plusMinutes(5));
        reservation = reservationJpaRepository.save(reservation);
        reservationId = reservation.getId();

        // 포인트 생성 (50000원)
        PointJpaEntity point = new PointJpaEntity(userId, new BigDecimal("50000"));
        pointJpaRepository.save(point);

        // Redis 기반 토큰 생성
        QueueToken queueToken = new QueueToken(userId, 1L, LocalDateTime.now().plusMinutes(30));
        queueToken.activate();
        QueueToken savedToken = queueTokenRepository.save(queueToken);
        token = savedToken.getToken();

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/payments")
    class ProcessPayment {

        @Test
        @DisplayName("정상 결제 요청 -> 200 OK, 결제 완료")
        void shouldProcessPaymentSuccessfully() throws Exception {
            PaymentController.PaymentRequest request = new PaymentController.PaymentRequest(userId, reservationId);

            mockMvc.perform(post("/api/v1/payments")
                    .header("Concert-Queue-Token", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.amount").value(10000));

            // DB 검증: 포인트 차감
            PointJpaEntity updatedPoint = pointJpaRepository.findByUserId(userId).orElseThrow();
            assertThat(updatedPoint.getBalance()).isEqualByComparingTo(new BigDecimal("40000"));

            // DB 검증: 예약 확정
            ReservationJpaEntity updatedReservation = reservationJpaRepository.findById(reservationId).orElseThrow();
            assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

            // DB 검증: 좌석 확정
            SeatJpaEntity updatedSeat = seatJpaRepository.findById(seatId).orElseThrow();
            assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        }

        @Test
        @DisplayName("잔액 부족 시 -> 400 Bad Request")
        void shouldReturn400_whenInsufficientBalance() throws Exception {
            // 포인트 잔액을 1000원으로 변경
            PointJpaEntity point = pointJpaRepository.findByUserId(userId).orElseThrow();
            point.setBalance(new BigDecimal("1000"));
            pointJpaRepository.save(point);
            entityManager.flush();
            entityManager.clear();

            PaymentController.PaymentRequest request = new PaymentController.PaymentRequest(userId, reservationId);

            mockMvc.perform(post("/api/v1/payments")
                    .header("Concert-Queue-Token", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("타인의 예약 결제 시도 -> 400 Bad Request")
        void shouldReturn400_whenNotOwner() throws Exception {
            Long otherUserId = 999L;
            PaymentController.PaymentRequest request = new PaymentController.PaymentRequest(otherUserId, reservationId);

            mockMvc.perform(post("/api/v1/payments")
                    .header("Concert-Queue-Token", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 예약 결제 시도 -> 404 Not Found")
        void shouldReturn404_whenReservationNotFound() throws Exception {
            PaymentController.PaymentRequest request = new PaymentController.PaymentRequest(userId, 999999L);

            mockMvc.perform(post("/api/v1/payments")
                    .header("Concert-Queue-Token", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
