package com.example.concert.domain.queue.infrastructure;

import com.example.concert.config.AbstractIntegrationTest;
import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("RedisQueueTokenRepositoryImpl 통합 테스트")
class RedisQueueTokenRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RedisQueueTokenRepositoryImpl repository;

    private static final Long USER_ID = 1L;
    private static final Long CONCERT_ID = 100L;

    @Nested
    @DisplayName("save()")
    class SaveTest {

        @Test
        @DisplayName("WAITING 상태 토큰 저장 시 대기열 Sorted Set에 추가된다")
        void saveWaitingToken_addedToSortedSet() {
            // given
            QueueToken token = new QueueToken(USER_ID, CONCERT_ID, LocalDateTime.now().plusMinutes(30));

            // when
            QueueToken saved = repository.save(token);

            // then
            assertThat(saved.getToken()).isNotBlank();
            assertThat(saved.getStatus()).isEqualTo(TokenStatus.WAITING);
            assertThat(saved.getScore()).isNotNull();

            // Redis에 저장 확인
            Optional<QueueToken> found = repository.findByToken(saved.getToken());
            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("ACTIVE 상태 토큰 저장 시 Active Set에 추가된다")
        void saveActiveToken_addedToActiveSet() {
            // given
            QueueToken token = new QueueToken(USER_ID, CONCERT_ID, LocalDateTime.now().plusMinutes(30));
            token.activate();

            // when
            QueueToken saved = repository.save(token);

            // then
            assertThat(saved.getStatus()).isEqualTo(TokenStatus.ACTIVE);

            long activeCount = repository.countByStatusAndConcertId(TokenStatus.ACTIVE, CONCERT_ID);
            assertThat(activeCount).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getRankByToken()")
    class RankTest {

        @Test
        @DisplayName("대기열의 순위를 정확하게 반환한다 (0-indexed)")
        void getRankByToken_returnsCorrectRank() {
            // given - 3개의 토큰을 순차적으로 추가
            QueueToken token1 = repository.save(new QueueToken(1L, CONCERT_ID, LocalDateTime.now().plusMinutes(30)));
            QueueToken token2 = repository.save(new QueueToken(2L, CONCERT_ID, LocalDateTime.now().plusMinutes(30)));
            QueueToken token3 = repository.save(new QueueToken(3L, CONCERT_ID, LocalDateTime.now().plusMinutes(30)));

            // when
            Long rank1 = repository.getRankByToken(token1.getToken(), CONCERT_ID);
            Long rank2 = repository.getRankByToken(token2.getToken(), CONCERT_ID);
            Long rank3 = repository.getRankByToken(token3.getToken(), CONCERT_ID);

            // then
            assertThat(rank1).isEqualTo(0L); // 0-indexed
            assertThat(rank2).isEqualTo(1L);
            assertThat(rank3).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("countByStatusAndConcertId()")
    class CountTest {

        @Test
        @DisplayName("WAITING 상태 토큰 수를 정확하게 반환한다")
        void countWaiting_returnsCorrectCount() {
            // given
            repository.save(new QueueToken(1L, CONCERT_ID, LocalDateTime.now().plusMinutes(30)));
            repository.save(new QueueToken(2L, CONCERT_ID, LocalDateTime.now().plusMinutes(30)));

            // when
            long count = repository.countByStatusAndConcertId(TokenStatus.WAITING, CONCERT_ID);

            // then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findTopNByStatusAndConcertIdOrderByIdAsc()")
    class FindTopNTest {

        @Test
        @DisplayName("상위 N개의 WAITING 토큰을 score 순으로 반환한다")
        void findTopN_returnsTokensInOrder() {
            // given
            QueueToken token1 = repository.save(new QueueToken(1L, CONCERT_ID, LocalDateTime.now().plusMinutes(30)));
            QueueToken token2 = repository.save(new QueueToken(2L, CONCERT_ID, LocalDateTime.now().plusMinutes(30)));
            QueueToken token3 = repository.save(new QueueToken(3L, CONCERT_ID, LocalDateTime.now().plusMinutes(30)));

            // when
            List<QueueToken> topTokens = repository.findTopNByStatusAndConcertIdOrderByIdAsc(
                    TokenStatus.WAITING, CONCERT_ID, 2);

            // then
            assertThat(topTokens).hasSize(2);
            assertThat(topTokens.get(0).getToken()).isEqualTo(token1.getToken());
            assertThat(topTokens.get(1).getToken()).isEqualTo(token2.getToken());
        }
    }

    @Nested
    @DisplayName("getActiveConcertIds()")
    class ActiveConcertIdsTest {

        @Test
        @DisplayName("대기열이 있는 콘서트 ID 목록을 반환한다")
        void getActiveConcertIds_returnsCorrectIds() {
            // given
            repository.save(new QueueToken(1L, 100L, LocalDateTime.now().plusMinutes(30)));
            repository.save(new QueueToken(2L, 200L, LocalDateTime.now().plusMinutes(30)));

            // when
            Set<Long> concertIds = repository.getActiveConcertIds();

            // then
            assertThat(concertIds).contains(100L, 200L);
        }
    }

    @Nested
    @DisplayName("토큰 상태 전환")
    class StatusTransitionTest {

        @Test
        @DisplayName("WAITING → ACTIVE 전환 시 대기열에서 제거되고 Active Set에 추가된다")
        void activate_removesFromWaitingAndAddsToActive() {
            // given
            QueueToken token = repository
                    .save(new QueueToken(USER_ID, CONCERT_ID, LocalDateTime.now().plusMinutes(30)));
            long waitingBefore = repository.countByStatusAndConcertId(TokenStatus.WAITING, CONCERT_ID);

            // when
            token.activate();
            repository.save(token);

            // then
            long waitingAfter = repository.countByStatusAndConcertId(TokenStatus.WAITING, CONCERT_ID);
            long activeAfter = repository.countByStatusAndConcertId(TokenStatus.ACTIVE, CONCERT_ID);

            assertThat(waitingAfter).isEqualTo(waitingBefore - 1);
            assertThat(activeAfter).isGreaterThanOrEqualTo(1);

            // 토큰 상태 확인
            Optional<QueueToken> found = repository.findByToken(token.getToken());
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(TokenStatus.ACTIVE);
        }
    }
}
