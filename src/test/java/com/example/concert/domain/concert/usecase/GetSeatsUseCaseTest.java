package com.example.concert.domain.concert.usecase;

import com.example.concert.domain.concert.entity.Seat;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.repository.SeatRepository;
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
    private SeatRepository seatRepository;

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
            List<Seat> allSeats = List.of(
                    new Seat(1L, SCHEDULE_ID, 1, BigDecimal.valueOf(10000), SeatStatus.AVAILABLE, 0L, null, null),
                    new Seat(2L, SCHEDULE_ID, 2, BigDecimal.valueOf(10000), SeatStatus.RESERVED, 0L, null, null));
            when(seatRepository.findAllByScheduleId(SCHEDULE_ID)).thenReturn(allSeats);

            // when
            GetSeatsUseCase.SeatsResult result = getSeatsUseCase.execute(VALID_TOKEN, SCHEDULE_ID, null);

            // then
            assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
            assertThat(result.seats()).hasSize(2);
            verify(validateTokenUseCase).execute(VALID_TOKEN);
            verify(seatRepository).findAllByScheduleId(SCHEDULE_ID);
            verify(seatRepository, never()).findAllByScheduleIdAndStatusIn(anyLong(), anyList());
        }

        @Test
        @DisplayName("상태 파라미터로 AVAILABLE 지정 시 해당 상태 좌석만 조회")
        void shouldReturnFilteredSeats_WhenStatusProvided() {
            // given
            List<SeatStatus> statuses = List.of(SeatStatus.AVAILABLE);
            List<Seat> availableSeats = List.of(
                    new Seat(1L, SCHEDULE_ID, 1, BigDecimal.valueOf(10000), SeatStatus.AVAILABLE, 0L, null, null));
            when(seatRepository.findAllByScheduleIdAndStatusIn(SCHEDULE_ID, statuses)).thenReturn(availableSeats);

            // when
            GetSeatsUseCase.SeatsResult result = getSeatsUseCase.execute(VALID_TOKEN, SCHEDULE_ID, statuses);

            // then
            assertThat(result.seats()).hasSize(1);
            assertThat(result.seats().get(0).status()).isEqualTo(SeatStatus.AVAILABLE);
            verify(seatRepository).findAllByScheduleIdAndStatusIn(SCHEDULE_ID, statuses);
            verify(seatRepository, never()).findAllByScheduleId(anyLong());
        }

        @Test
        @DisplayName("빈 상태 리스트 전달 시 전체 좌석 조회")
        void shouldReturnAllSeats_WhenEmptyStatusList() {
            // given
            List<Seat> allSeats = List.of(
                    new Seat(1L, SCHEDULE_ID, 1, BigDecimal.valueOf(10000), SeatStatus.AVAILABLE, 0L, null, null));
            when(seatRepository.findAllByScheduleId(SCHEDULE_ID)).thenReturn(allSeats);

            // when
            GetSeatsUseCase.SeatsResult result = getSeatsUseCase.execute(VALID_TOKEN, SCHEDULE_ID, List.of());

            // then
            verify(seatRepository).findAllByScheduleId(SCHEDULE_ID);
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

            verify(seatRepository, never()).findAllByScheduleId(anyLong());
        }
    }
}
