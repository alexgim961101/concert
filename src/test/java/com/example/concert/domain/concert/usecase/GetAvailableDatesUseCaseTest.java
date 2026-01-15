package com.example.concert.domain.concert.usecase;

import com.example.concert.domain.concert.entity.ConcertSchedule;
import com.example.concert.domain.concert.entity.SeatStatus;
import com.example.concert.domain.concert.repository.ConcertScheduleRepository;
import com.example.concert.domain.concert.repository.SeatRepository;
import com.example.concert.domain.queue.usecase.ValidateTokenUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAvailableDatesUseCase 단위 테스트")
class GetAvailableDatesUseCaseTest {

    @Mock
    private ValidateTokenUseCase validateTokenUseCase;

    @Mock
    private ConcertScheduleRepository concertScheduleRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private GetAvailableDatesUseCase getAvailableDatesUseCase;

    private ConcertSchedule createSchedule(Long id, Long concertId, LocalDateTime concertDate) {
        return new ConcertSchedule(id, concertId, concertDate, LocalDateTime.now().minusDays(1), null, null);
    }

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

            ConcertSchedule schedule1 = createSchedule(1L, concertId, LocalDateTime.of(2024, 5, 1, 19, 0));
            ConcertSchedule schedule2 = createSchedule(2L, concertId, LocalDateTime.of(2024, 5, 2, 19, 0));
            when(concertScheduleRepository.findByConcertId(concertId)).thenReturn(List.of(schedule1, schedule2));

            when(seatRepository.countByScheduleIdAndStatus(1L, SeatStatus.AVAILABLE)).thenReturn(50);
            when(seatRepository.countByScheduleIdAndStatus(2L, SeatStatus.AVAILABLE)).thenReturn(0);

            GetAvailableDatesUseCase.AvailableDatesResult result = getAvailableDatesUseCase.execute(token, concertId);

            assertThat(result.concertId()).isEqualTo(concertId);
            assertThat(result.schedules()).hasSize(2);
            assertThat(result.schedules().get(0).availableSeats()).isEqualTo(50);
            assertThat(result.schedules().get(1).availableSeats()).isEqualTo(0);
        }

        @Test
        @DisplayName("스케줄이 없는 콘서트도 정상 처리")
        void shouldReturnEmptySchedules_whenConcertHasNoSchedules() {
            Long concertId = 1L;
            String token = "valid-token";

            when(validateTokenUseCase.execute(token)).thenReturn(null);
            when(concertScheduleRepository.existsConcertById(concertId)).thenReturn(true);
            when(concertScheduleRepository.findByConcertId(concertId)).thenReturn(List.of());

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
