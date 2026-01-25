package com.example.concert.domain.queue.usecase;

import com.example.concert.domain.concert.infrastructure.ConcertJpaEntity;
import com.example.concert.domain.concert.infrastructure.ConcertJpaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivateTokenUseCase 단위 테스트")
class ActivateTokenUseCaseTest {

    @Mock
    private QueueTokenRepository queueTokenRepository;

    @Mock
    private ConcertJpaRepository concertJpaRepository;

    @InjectMocks
    private ActivateTokenUseCase activateTokenUseCase;

    @Test
    @DisplayName("활성화 가능한 슬롯이 있으면 WAITING 토큰을 ACTIVE로 전환")
    void shouldActivateTokens_whenSlotsAvailable() {
        // Given
        Long concertId = 1L;
        // ConcertJpaEntity is mocked directly below
        // Reflection or setter to set ID if needed, but here assuming simple mock or
        // minimal impl
        // Since ConcertJpaEntity probably has private fields and no setter/builder
        // exposed in test usually,
        // we might need to mock findAll() result properly.
        // For simplicity, let's assume we can set ID or mock the object.
        // But ConcertJpaEntity might be hard to instantiate properly if it's protected.
        // Let's use mock for the list element.
        ConcertJpaEntity mockConcert = mock(ConcertJpaEntity.class);
        when(mockConcert.getId()).thenReturn(concertId);

        when(concertJpaRepository.findAll()).thenReturn(List.of(mockConcert));

        // Active tokens: 40 (Max 50) -> 10 slots available
        when(queueTokenRepository.countByStatusAndConcertId(TokenStatus.ACTIVE, concertId)).thenReturn(40L);

        // 10 waiting tokens found
        QueueToken waitingToken1 = new QueueToken(1L, 1L, concertId, "token1", TokenStatus.WAITING,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now());
        QueueToken waitingToken2 = new QueueToken(2L, 2L, concertId, "token2", TokenStatus.WAITING,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now());

        // Mocking spy or just object state check.
        // QueueToken is domain object, state change is visible.

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
        ConcertJpaEntity mockConcert = mock(ConcertJpaEntity.class);
        when(mockConcert.getId()).thenReturn(concertId);

        when(concertJpaRepository.findAll()).thenReturn(List.of(mockConcert));

        // Active tokens: 50 (Max 50) -> 0 slots
        when(queueTokenRepository.countByStatusAndConcertId(TokenStatus.ACTIVE, concertId)).thenReturn(50L);

        // When
        int activatedCount = activateTokenUseCase.execute();

        // Then
        assertThat(activatedCount).isEqualTo(0);
        verify(queueTokenRepository, never()).findTopNByStatusAndConcertIdOrderByIdAsc(any(), any(), anyInt());
        verify(queueTokenRepository, never()).save(any());
    }
}
