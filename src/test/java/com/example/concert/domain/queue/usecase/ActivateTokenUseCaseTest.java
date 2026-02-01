package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivateTokenUseCase 단위 테스트")
class ActivateTokenUseCaseTest {

    @Mock
    private QueueTokenRepository queueTokenRepository;

    @InjectMocks
    private ActivateTokenUseCase activateTokenUseCase;

    @Test
    @DisplayName("활성화 가능한 슬롯이 있으면 WAITING 토큰을 ACTIVE로 전환")
    void shouldActivateTokens_whenSlotsAvailable() {
        // Given
        Long concertId = 1L;

        // Redis 기반 활성 콘서트 ID 조회 Mock
        when(queueTokenRepository.getActiveConcertIds()).thenReturn(Set.of(concertId));

        // Active tokens: 40 (Max 50) -> 10 slots available
        when(queueTokenRepository.countByStatusAndConcertId(TokenStatus.ACTIVE, concertId)).thenReturn(40L);

        // 2 waiting tokens found
        QueueToken waitingToken1 = new QueueToken(1L, 1L, concertId, "token1", TokenStatus.WAITING,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now());
        QueueToken waitingToken2 = new QueueToken(2L, 2L, concertId, "token2", TokenStatus.WAITING,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now());

        when(queueTokenRepository.findTopNByStatusAndConcertIdOrderByIdAsc(TokenStatus.WAITING, concertId, 10))
                .thenReturn(List.of(waitingToken1, waitingToken2));

        // When
        int activatedCount = activateTokenUseCase.execute();

        // Then
        assertThat(activatedCount).isEqualTo(2);
        assertThat(waitingToken1.getStatus()).isEqualTo(TokenStatus.ACTIVE);
        assertThat(waitingToken2.getStatus()).isEqualTo(TokenStatus.ACTIVE);

        verify(queueTokenRepository, times(2)).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("활성화 슬롯이 없으면 아무것도 하지 않음")
    void shouldNotActivate_whenNoSlotsAvailable() {
        // Given
        Long concertId = 1L;

        when(queueTokenRepository.getActiveConcertIds()).thenReturn(Set.of(concertId));

        // Active tokens: 50 (Max 50) -> 0 slots
        when(queueTokenRepository.countByStatusAndConcertId(TokenStatus.ACTIVE, concertId)).thenReturn(50L);

        // When
        int activatedCount = activateTokenUseCase.execute();

        // Then
        assertThat(activatedCount).isEqualTo(0);
        verify(queueTokenRepository, never()).findTopNByStatusAndConcertIdOrderByIdAsc(any(), any(), anyInt());
        verify(queueTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 콘서트가 없으면 아무것도 하지 않음")
    void shouldNotActivate_whenNoActiveConcerts() {
        // Given
        when(queueTokenRepository.getActiveConcertIds()).thenReturn(Set.of());

        // When
        int activatedCount = activateTokenUseCase.execute();

        // Then
        assertThat(activatedCount).isEqualTo(0);
        verify(queueTokenRepository, never()).countByStatusAndConcertId(any(), any());
    }
}
