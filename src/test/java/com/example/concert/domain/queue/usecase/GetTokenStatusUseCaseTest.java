package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTokenStatusUseCase 단위 테스트")
class GetTokenStatusUseCaseTest {

    @Mock
    private QueueTokenRepository queueTokenRepository;

    @InjectMocks
    private GetTokenStatusUseCase getTokenStatusUseCase;

    private QueueToken createToken(Long id, TokenStatus status) {
        return new QueueToken(id, 1L, 1L, "test-token", status,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now());
    }

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {

        @Test
        @DisplayName("ACTIVE 상태일 때 rank는 0")
        void shouldReturnRankZero_whenStatusIsActive() {
            String tokenValue = "active-token";
            QueueToken activeToken = createToken(1L, TokenStatus.ACTIVE);
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(activeToken));

            GetTokenStatusUseCase.TokenStatusResult result = getTokenStatusUseCase.execute(tokenValue);

            assertThat(result.status()).isEqualTo("ACTIVE");
            assertThat(result.rank()).isEqualTo(0);
            assertThat(result.estimatedWaitTime()).isEqualTo(0);
        }

        @Test
        @DisplayName("EXPIRED 상태일 때 rank는 0")
        void shouldReturnRankZero_whenStatusIsExpired() {
            String tokenValue = "expired-token";
            QueueToken expiredToken = createToken(1L, TokenStatus.EXPIRED);
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(expiredToken));

            GetTokenStatusUseCase.TokenStatusResult result = getTokenStatusUseCase.execute(tokenValue);

            assertThat(result.status()).isEqualTo("EXPIRED");
            assertThat(result.rank()).isEqualTo(0);
            assertThat(result.estimatedWaitTime()).isEqualTo(0);
        }

        @Test
        @DisplayName("WAITING 상태일 때 앞에 3명이면 rank는 4")
        void shouldReturnCalculatedRank_whenStatusIsWaiting() {
            String tokenValue = "waiting-token";
            QueueToken waitingToken = createToken(10L, TokenStatus.WAITING);
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(waitingToken));
            when(queueTokenRepository.countByStatusAndConcertIdAndIdLessThan(eq(TokenStatus.WAITING), eq(1L), eq(10L)))
                    .thenReturn(3L);

            GetTokenStatusUseCase.TokenStatusResult result = getTokenStatusUseCase.execute(tokenValue);

            assertThat(result.status()).isEqualTo("WAITING");
            assertThat(result.rank()).isEqualTo(4L);
            assertThat(result.estimatedWaitTime()).isEqualTo(8L); // 4 * 2 seconds
        }

        @Test
        @DisplayName("WAITING 상태일 때 앞에 아무도 없으면 rank는 1")
        void shouldReturnRankOne_whenNoOneAhead() {
            String tokenValue = "first-waiting-token";
            QueueToken waitingToken = createToken(1L, TokenStatus.WAITING);
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(waitingToken));
            when(queueTokenRepository.countByStatusAndConcertIdAndIdLessThan(eq(TokenStatus.WAITING), eq(1L), eq(1L)))
                    .thenReturn(0L);

            GetTokenStatusUseCase.TokenStatusResult result = getTokenStatusUseCase.execute(tokenValue);

            assertThat(result.rank()).isEqualTo(1L);
            assertThat(result.estimatedWaitTime()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCase {

        @Test
        @DisplayName("토큰이 null인 경우 예외 발생")
        void shouldThrowException_whenTokenIsNull() {
            assertThatThrownBy(() -> getTokenStatusUseCase.execute(null))
                    .isInstanceOf(TokenNotFoundException.class);
        }

        @Test
        @DisplayName("토큰이 빈 문자열인 경우 예외 발생")
        void shouldThrowException_whenTokenIsEmpty() {
            assertThatThrownBy(() -> getTokenStatusUseCase.execute(""))
                    .isInstanceOf(TokenNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 토큰인 경우 예외 발생")
        void shouldThrowException_whenTokenNotFound() {
            String tokenValue = "non-existent-token";
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> getTokenStatusUseCase.execute(tokenValue))
                    .isInstanceOf(TokenNotFoundException.class);
        }
    }
}
