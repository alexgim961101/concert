package com.example.concert.domain.concert.usecase;

import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.service.ConcertService;
import com.example.concert.domain.concert.service.ConcertService.SeatInfo;
import com.example.concert.domain.concert.service.ConcertService.SeatsResult;
import com.example.concert.domain.queue.usecase.TokenNotFoundException;
import com.example.concert.domain.queue.usecase.ValidateTokenUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetSeatsUseCase 단위 테스트")
class GetSeatsUseCaseTest {

    @Mock
    private ValidateTokenUseCase validateTokenUseCase;

    @Mock
    private ConcertService concertService;

    @InjectMocks
    private GetSeatsUseCase getSeatsUseCase;

    private final String VALID_TOKEN = "valid-token";
    private final Long SCHEDULE_ID = 1L;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("상태 파라미터 없이 호출 시 전체 좌석 조회")
        void shouldReturnAllSeats_WhenNoStatusProvided() {
            // given
            List<SeatInfo> allSeats = List.of(
                    new SeatInfo(1L, 1, SeatStatus.AVAILABLE, BigDecimal.valueOf(10000)),
                    new SeatInfo(2L, 2, SeatStatus.RESERVED, BigDecimal.valueOf(10000)));
            SeatsResult seatsResult = new SeatsResult(SCHEDULE_ID, allSeats);
            when(concertService.getSeats(SCHEDULE_ID, null)).thenReturn(seatsResult);

            // when
            GetSeatsUseCase.SeatsResult result = getSeatsUseCase.execute(VALID_TOKEN, SCHEDULE_ID, null);

            // then
            assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
            assertThat(result.seats()).hasSize(2);
            verify(validateTokenUseCase).execute(VALID_TOKEN);
            verify(concertService).getSeats(SCHEDULE_ID, null);
        }

        @Test
        @DisplayName("상태 파라미터로 AVAILABLE 지정 시 해당 상태 좌석만 조회")
        void shouldReturnFilteredSeats_WhenStatusProvided() {
            // given
            List<SeatStatus> statuses = List.of(SeatStatus.AVAILABLE);
            List<SeatInfo> availableSeats = List.of(
                    new SeatInfo(1L, 1, SeatStatus.AVAILABLE, BigDecimal.valueOf(10000)));
            SeatsResult seatsResult = new SeatsResult(SCHEDULE_ID, availableSeats);
            when(concertService.getSeats(SCHEDULE_ID, statuses)).thenReturn(seatsResult);

            // when
            GetSeatsUseCase.SeatsResult result = getSeatsUseCase.execute(VALID_TOKEN, SCHEDULE_ID, statuses);

            // then
            assertThat(result.seats()).hasSize(1);
            assertThat(result.seats().get(0).status()).isEqualTo(SeatStatus.AVAILABLE);
            verify(concertService).getSeats(SCHEDULE_ID, statuses);
        }

        @Test
        @DisplayName("빈 상태 리스트 전달 시 전체 좌석 조회")
        void shouldReturnAllSeats_WhenEmptyStatusList() {
            // given
            List<SeatInfo> allSeats = List.of(
                    new SeatInfo(1L, 1, SeatStatus.AVAILABLE, BigDecimal.valueOf(10000)));
            SeatsResult seatsResult = new SeatsResult(SCHEDULE_ID, allSeats);
            when(concertService.getSeats(SCHEDULE_ID, List.of())).thenReturn(seatsResult);

            // when
            GetSeatsUseCase.SeatsResult result = getSeatsUseCase.execute(VALID_TOKEN, SCHEDULE_ID, List.of());

            // then
            verify(concertService).getSeats(SCHEDULE_ID, List.of());
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCases {

        @Test
        @DisplayName("유효하지 않은 토큰으로 호출 시 예외 발생")
        void shouldThrowException_WhenInvalidToken() {
            // given
            doThrow(new TokenNotFoundException("Token not found"))
                    .when(validateTokenUseCase).execute("invalid-token");

            // when & then
            assertThatThrownBy(() -> getSeatsUseCase.execute("invalid-token", SCHEDULE_ID, null))
                    .isInstanceOf(TokenNotFoundException.class);

            verify(concertService, never()).getSeats(anyLong(), anyList());
        }
    }
}
