package com.example.concert.domain.concert.usecase;

import com.example.concert.domain.concert.repository.ConcertScheduleRepository;
import com.example.concert.domain.concert.service.ConcertService;
import com.example.concert.domain.concert.service.ConcertService.ScheduleInfo;
import com.example.concert.domain.concert.service.ConcertService.SchedulesWithSeatsResult;
import com.example.concert.domain.queue.usecase.ValidateTokenUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAvailableDatesUseCase 단위 테스트")
class GetAvailableDatesUseCaseTest {

    @Mock
    private ValidateTokenUseCase validateTokenUseCase;

    @Mock
    private ConcertScheduleRepository concertScheduleRepository;

    @Mock
    private ConcertService concertService;

    @InjectMocks
    private GetAvailableDatesUseCase getAvailableDatesUseCase;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {
        @Test
        @DisplayName("스케줄과 가용 좌석 수가 정확히 반환됨")
        void shouldReturnSchedulesWithAvailableSeats() {
            Long concertId = 1L;
            String token = "valid-token";

            when(validateTokenUseCase.execute(token)).thenReturn(null);
            when(concertScheduleRepository.existsConcertById(concertId)).thenReturn(true);

            // ConcertService 모킹
            List<ScheduleInfo> scheduleInfos = List.of(
                    new ScheduleInfo(1L, LocalDate.of(2024, 5, 1), 50),
                    new ScheduleInfo(2L, LocalDate.of(2024, 5, 2), 0));
            SchedulesWithSeatsResult serviceResult = new SchedulesWithSeatsResult(concertId, scheduleInfos);
            when(concertService.getSchedulesWithSeats(concertId)).thenReturn(serviceResult);

            GetAvailableDatesUseCase.AvailableDatesResult result = getAvailableDatesUseCase.execute(token, concertId);

            assertThat(result.concertId()).isEqualTo(concertId);
            assertThat(result.schedules()).hasSize(2);
            assertThat(result.schedules().get(0).availableSeats()).isEqualTo(50);
            assertThat(result.schedules().get(1).availableSeats()).isEqualTo(0);

            verify(validateTokenUseCase).execute(token);
            verify(concertService).getSchedulesWithSeats(concertId);
        }

        @Test
        @DisplayName("스케줄이 없는 콘서트도 정상 처리")
        void shouldReturnEmptySchedules_whenConcertHasNoSchedules() {
            Long concertId = 1L;
            String token = "valid-token";

            when(validateTokenUseCase.execute(token)).thenReturn(null);
            when(concertScheduleRepository.existsConcertById(concertId)).thenReturn(true);

            // 빈 스케줄 반환
            SchedulesWithSeatsResult serviceResult = new SchedulesWithSeatsResult(concertId, List.of());
            when(concertService.getSchedulesWithSeats(concertId)).thenReturn(serviceResult);

            GetAvailableDatesUseCase.AvailableDatesResult result = getAvailableDatesUseCase.execute(token, concertId);

            assertThat(result.concertId()).isEqualTo(concertId);
            assertThat(result.schedules()).isEmpty();
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCase {
        @Test
        @DisplayName("존재하지 않는 콘서트 ID인 경우")
        void shouldThrowException_whenConcertNotFound() {
            Long concertId = 999L;
            String token = "valid-token";

            when(validateTokenUseCase.execute(token)).thenReturn(null);
            when(concertScheduleRepository.existsConcertById(concertId)).thenReturn(false);

            assertThatThrownBy(() -> getAvailableDatesUseCase.execute(token, concertId))
                    .isInstanceOf(ConcertNotFoundException.class);
        }
    }
}
