package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IssueTokenUseCase 단위 테스트")
class IssueTokenUseCaseTest {

    @Mock
    private QueueTokenRepository queueTokenRepository;

    @InjectMocks
    private IssueTokenUseCase issueTokenUseCase;

    private QueueToken createSavedToken(Long id, Long userId, Long concertId, String token) {
        return new QueueToken(id, userId, concertId, token, TokenStatus.WAITING,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now());
    }

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {

        @Test
        @DisplayName("토큰 발급 - 대기열에 아무도 없는 경우 rank는 1")
        void shouldIssueToken_whenQueueIsEmpty() {
            Long userId = 1L;
            Long concertId = 1L;
            QueueToken savedToken = createSavedToken(1L, userId, concertId, "uuid-token");

            when(queueTokenRepository.save(any(QueueToken.class))).thenReturn(savedToken);
            when(queueTokenRepository.countByStatusAndConcertIdAndIdLessThan(eq(TokenStatus.WAITING), eq(concertId),
                    eq(1L))).thenReturn(0L);

            IssueTokenUseCase.IssueTokenResult result = issueTokenUseCase.execute(userId, concertId);

            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo("uuid-token");
            assertThat(result.status()).isEqualTo("WAITING");
            assertThat(result.rank()).isEqualTo(1L);
            assertThat(result.estimatedWaitTime()).isEqualTo(2L); // 1 * 2 seconds
        }

        @Test
        @DisplayName("토큰 발급 - 대기열에 5명이 있는 경우 rank는 6")
        void shouldIssueToken_whenQueueHas5Users() {
            Long userId = 6L;
            Long concertId = 1L;
            QueueToken savedToken = createSavedToken(6L, userId, concertId, "uuid-token-6");

            when(queueTokenRepository.save(any(QueueToken.class))).thenReturn(savedToken);
            when(queueTokenRepository.countByStatusAndConcertIdAndIdLessThan(eq(TokenStatus.WAITING), eq(concertId),
                    eq(6L))).thenReturn(5L);

            IssueTokenUseCase.IssueTokenResult result = issueTokenUseCase.execute(userId, concertId);

            assertThat(result.rank()).isEqualTo(6L);
            assertThat(result.estimatedWaitTime()).isEqualTo(12L); // 6 * 2 seconds
        }

        @Test
        @DisplayName("토큰 발급 시 QueueToken이 WAITING 상태로 저장됨")
        void shouldSaveToken_withWaitingStatus() {
            Long userId = 1L;
            Long concertId = 1L;
            QueueToken savedToken = createSavedToken(1L, userId, concertId, "uuid-token");

            when(queueTokenRepository.save(any(QueueToken.class))).thenReturn(savedToken);
            when(queueTokenRepository.countByStatusAndConcertIdAndIdLessThan(any(), any(), any())).thenReturn(0L);

            issueTokenUseCase.execute(userId, concertId);

            ArgumentCaptor<QueueToken> captor = ArgumentCaptor.forClass(QueueToken.class);
            verify(queueTokenRepository).save(captor.capture());

            QueueToken capturedToken = captor.getValue();
            assertThat(capturedToken.getUserId()).isEqualTo(userId);
            assertThat(capturedToken.getConcertId()).isEqualTo(concertId);
            assertThat(capturedToken.getStatus()).isEqualTo(TokenStatus.WAITING);
            assertThat(capturedToken.getToken()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCase {

        @Test
        @DisplayName("userId가 null인 경우 예외 발생")
        void shouldThrowException_whenUserIdIsNull() {
            assertThatThrownBy(() -> issueTokenUseCase.execute(null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("concertId가 null인 경우 예외 발생")
        void shouldThrowException_whenConcertIdIsNull() {
            assertThatThrownBy(() -> issueTokenUseCase.execute(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("concertId");
        }
    }
}
