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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateTokenUseCase 단위 테스트")
class ValidateTokenUseCaseTest {

    @Mock
    private QueueTokenRepository queueTokenRepository;

    @InjectMocks
    private ValidateTokenUseCase validateTokenUseCase;

    private QueueToken createToken(TokenStatus status, LocalDateTime expiresAt) {
        return new QueueToken(1L, 1L, 1L, "test-token", status, expiresAt, LocalDateTime.now());
    }

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {
        @Test
        @DisplayName("유효한 토큰 - Active 상태, 만료 전")
        void shouldReturnToken_whenTokenIsValidAndActive() {
            String tokenValue = "valid-token";
            QueueToken validToken = createToken(TokenStatus.ACTIVE, LocalDateTime.now().plusHours(1));
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(validToken));

            QueueToken result = validateTokenUseCase.execute(tokenValue);

            assertThat(result).isNotNull();
            assertThat(result.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCase {
        @Test
        @DisplayName("토큰이 null인 경우")
        void shouldThrowException_whenTokenIsNull() {
            assertThatThrownBy(() -> validateTokenUseCase.execute(null))
                    .isInstanceOf(TokenNotFoundException.class);
        }

        @Test
        @DisplayName("토큰이 빈 문자열인 경우")
        void shouldThrowException_whenTokenIsEmpty() {
            assertThatThrownBy(() -> validateTokenUseCase.execute(""))
                    .isInstanceOf(TokenNotFoundException.class);
        }

        @Test
        @DisplayName("DB에 없는 토큰인 경우")
        void shouldThrowException_whenTokenNotFoundInDB() {
            String tokenValue = "non-existent-token";
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> validateTokenUseCase.execute(tokenValue))
                    .isInstanceOf(TokenNotFoundException.class);
        }

        @Test
        @DisplayName("토큰 상태가 WAITING인 경우")
        void shouldThrowException_whenTokenStatusIsWaiting() {
            String tokenValue = "waiting-token";
            QueueToken waitingToken = createToken(TokenStatus.WAITING, LocalDateTime.now().plusHours(1));
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(waitingToken));

            assertThatThrownBy(() -> validateTokenUseCase.execute(tokenValue))
                    .isInstanceOf(TokenNotActiveException.class);
        }

        @Test
        @DisplayName("토큰 상태가 EXPIRED인 경우")
        void shouldThrowException_whenTokenStatusIsExpired() {
            String tokenValue = "expired-token";
            QueueToken expiredToken = createToken(TokenStatus.EXPIRED, LocalDateTime.now().plusHours(1));
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> validateTokenUseCase.execute(tokenValue))
                    .isInstanceOf(TokenNotActiveException.class);
        }

        @Test
        @DisplayName("토큰이 만료된 경우 (Active이지만 시간 초과)")
        void shouldThrowException_whenTokenIsExpiredByTime() {
            String tokenValue = "time-expired-token";
            QueueToken expiredToken = createToken(TokenStatus.ACTIVE, LocalDateTime.now().minusMinutes(1));
            when(queueTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> validateTokenUseCase.execute(tokenValue))
                    .isInstanceOf(TokenExpiredException.class);
        }
    }
}
