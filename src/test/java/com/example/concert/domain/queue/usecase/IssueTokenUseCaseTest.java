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
            // 즉시 활성화 되므로 ACTIVE 상태여야 함
            QueueToken savedToken = new QueueToken(1L, userId, concertId, "uuid-token", TokenStatus.ACTIVE,
                    LocalDateTime.now().plusMinutes(30), LocalDateTime.now());

            when(queueTokenRepository.save(any(QueueToken.class))).thenReturn(savedToken);
            // Active 토큰이 0개 -> 50개 미만이므로 즉시 활성화
            when(queueTokenRepository.countByStatusAndConcertId(eq(TokenStatus.ACTIVE), eq(concertId))).thenReturn(0L);

            IssueTokenUseCase.IssueTokenResult result = issueTokenUseCase.execute(userId, concertId);

            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo("uuid-token");
            assertThat(result.status()).isEqualTo("ACTIVE");
            assertThat(result.rank()).isEqualTo(0L); // ACTIVE라 rank 0
            assertThat(result.estimatedWaitTime()).isEqualTo(0L); // ACTIVE라 waitTime 0
        }

        @Test
        @DisplayName("토큰 발급 - 활성 슬롯이 꽉 찬 경우 -> WAITING 상태 및 대기열 순위 반환")
        void shouldReturnWaitingRank_whenSlotsFull() {
            Long userId = 6L;
            Long concertId = 1L;
            QueueToken savedToken = createSavedToken(6L, userId, concertId, "uuid-token-6");

            when(queueTokenRepository.save(any(QueueToken.class))).thenReturn(savedToken);

            // Active가 50개 꽉 차있다고 가정 -> WAITING 상태 유지
            when(queueTokenRepository.countByStatusAndConcertId(eq(TokenStatus.ACTIVE), eq(concertId))).thenReturn(50L);

            // Redis ZRANK 기반 순위 조회 (0-indexed, 5는 6번째)
            when(queueTokenRepository.getRankByToken(eq("uuid-token-6"), eq(concertId))).thenReturn(5L);

            IssueTokenUseCase.IssueTokenResult result = issueTokenUseCase.execute(userId, concertId);

            assertThat(result.rank()).isEqualTo(6L); // 0-indexed 5 + 1 = 6
            assertThat(result.estimatedWaitTime()).isEqualTo(12L); // 6 * 2 seconds
            assertThat(result.status()).isEqualTo("WAITING");
        }

        @Test
        @DisplayName("토큰 발급 시 QueueToken이 WAITING 상태로 저장됨 (슬롯 부족 시)")
        void shouldSaveToken_withWaitingStatus_whenSlotsFull() {
            Long userId = 1L;
            Long concertId = 1L;
            QueueToken savedToken = createSavedToken(1L, userId, concertId, "uuid-token");

            when(queueTokenRepository.save(any(QueueToken.class))).thenReturn(savedToken);
            // Active가 50개 꽉 차있음
            when(queueTokenRepository.countByStatusAndConcertId(any(), any())).thenReturn(50L);
            when(queueTokenRepository.getRankByToken(any(), any())).thenReturn(0L);

            issueTokenUseCase.execute(userId, concertId);

            ArgumentCaptor<QueueToken> captor = ArgumentCaptor.forClass(QueueToken.class);
            verify(queueTokenRepository).save(captor.capture());

            QueueToken capturedToken = captor.getValue();
            assertThat(capturedToken.getUserId()).isEqualTo(userId);
            assertThat(capturedToken.getConcertId()).isEqualTo(concertId);
            assertThat(capturedToken.getStatus()).isEqualTo(TokenStatus.WAITING); // 초기 생성 시 WAITING
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
