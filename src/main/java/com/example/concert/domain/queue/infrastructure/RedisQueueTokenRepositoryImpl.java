package com.example.concert.domain.queue.infrastructure;

import com.example.concert.domain.queue.entity.QueueToken;
import com.example.concert.domain.queue.entity.TokenStatus;
import com.example.concert.domain.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Redis 기반 QueueTokenRepository 구현체
 * 
 * 키 구조:
 * - queue:waiting:{concertId} → ZSET { token: score(timestamp) }
 * - queue:active:{concertId} → SET { token }
 * - queue:token:{token} → HASH { userId, concertId, status, expiresAt,
 * createdAt, score }
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisQueueTokenRepositoryImpl implements QueueTokenRepository {

    private static final String WAITING_KEY_PREFIX = "queue:waiting:";
    private static final String ACTIVE_KEY_PREFIX = "queue:active:";
    private static final String TOKEN_KEY_PREFIX = "queue:token:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RedisTemplate<String, String> queueRedisTemplate;

    @Override
    public QueueToken save(QueueToken queueToken) {
        String tokenKey = TOKEN_KEY_PREFIX + queueToken.getToken();
        Long concertId = queueToken.getConcertId();

        // score가 null이면 현재 시간으로 생성
        long score = queueToken.getScore() != null ? queueToken.getScore() : System.currentTimeMillis();

        // Hash에 토큰 메타데이터 저장
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("userId", String.valueOf(queueToken.getUserId()));
        tokenData.put("concertId", String.valueOf(concertId));
        tokenData.put("status", queueToken.getStatus().name());
        tokenData.put("score", String.valueOf(score));
        if (queueToken.getExpiresAt() != null) {
            tokenData.put("expiresAt", queueToken.getExpiresAt().format(DATE_FORMATTER));
        }
        tokenData.put("createdAt", LocalDateTime.now().format(DATE_FORMATTER));

        queueRedisTemplate.opsForHash().putAll(tokenKey, tokenData);

        // 상태에 따라 적절한 Set에 추가
        if (queueToken.getStatus() == TokenStatus.WAITING) {
            // 대기열 Sorted Set에 추가
            queueRedisTemplate.opsForZSet().add(
                    WAITING_KEY_PREFIX + concertId,
                    queueToken.getToken(),
                    score);
            // Active Set에서 제거 (상태 변경 대비)
            queueRedisTemplate.opsForSet().remove(ACTIVE_KEY_PREFIX + concertId, queueToken.getToken());
        } else if (queueToken.getStatus() == TokenStatus.ACTIVE) {
            // Active Set에 추가
            queueRedisTemplate.opsForSet().add(ACTIVE_KEY_PREFIX + concertId, queueToken.getToken());
            // 대기열에서 제거
            queueRedisTemplate.opsForZSet().remove(WAITING_KEY_PREFIX + concertId, queueToken.getToken());
        } else if (queueToken.getStatus() == TokenStatus.EXPIRED) {
            // 모든 Set에서 제거
            queueRedisTemplate.opsForZSet().remove(WAITING_KEY_PREFIX + concertId, queueToken.getToken());
            queueRedisTemplate.opsForSet().remove(ACTIVE_KEY_PREFIX + concertId, queueToken.getToken());
        }

        log.debug("Saved queue token: {} with status: {}", queueToken.getToken(), queueToken.getStatus());

        return new QueueToken(
                null, // Redis에서는 id 미사용
                score,
                queueToken.getUserId(),
                queueToken.getConcertId(),
                queueToken.getToken(),
                queueToken.getStatus(),
                queueToken.getExpiresAt(),
                LocalDateTime.now());
    }

    @Override
    public Optional<QueueToken> findByToken(String token) {
        String tokenKey = TOKEN_KEY_PREFIX + token;

        Map<Object, Object> entries = queueRedisTemplate.opsForHash().entries(tokenKey);
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapToQueueToken(token, entries));
    }

    @Override
    public long countByStatusAndIdLessThan(TokenStatus status, Long id) {
        // Redis에서는 id 기반 카운트가 불가능, 이 메서드는 Redis 방식에서는 사용되지 않음
        // 호환성을 위해 0 반환
        log.warn("countByStatusAndIdLessThan is not supported in Redis implementation");
        return 0;
    }

    @Override
    public long countByStatusAndConcertIdAndIdLessThan(TokenStatus status, Long concertId, Long id) {
        // Redis에서는 score 기반으로 순위를 계산
        // 이 메서드는 UseCase에서 직접 ZRANK를 호출하도록 리팩토링 필요
        // 현재는 호환성을 위해 기존 로직 유지
        log.warn(
                "countByStatusAndConcertIdAndIdLessThan is deprecated in Redis implementation. Use getRankByToken instead.");
        return 0;
    }

    /**
     * 토큰의 대기열 순위를 반환 (0-indexed → 1-indexed 변환은 호출측에서)
     */
    public Long getRankByToken(String token, Long concertId) {
        return queueRedisTemplate.opsForZSet().rank(WAITING_KEY_PREFIX + concertId, token);
    }

    @Override
    public long countByStatusAndConcertId(TokenStatus status, Long concertId) {
        if (status == TokenStatus.WAITING) {
            Long size = queueRedisTemplate.opsForZSet().size(WAITING_KEY_PREFIX + concertId);
            return size != null ? size : 0;
        } else if (status == TokenStatus.ACTIVE) {
            Long size = queueRedisTemplate.opsForSet().size(ACTIVE_KEY_PREFIX + concertId);
            return size != null ? size : 0;
        }
        return 0;
    }

    @Override
    public List<QueueToken> findTopNByStatusAndConcertIdOrderByIdAsc(TokenStatus status, Long concertId, int limit) {
        if (status != TokenStatus.WAITING) {
            log.warn("findTopNByStatusAndConcertIdOrderByIdAsc only supports WAITING status in Redis implementation");
            return List.of();
        }

        // ZRANGE로 상위 N개 토큰 조회 (score 오름차순)
        Set<String> tokens = queueRedisTemplate.opsForZSet()
                .range(WAITING_KEY_PREFIX + concertId, 0, limit - 1);

        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        List<QueueToken> result = new ArrayList<>();
        for (String token : tokens) {
            findByToken(token).ifPresent(result::add);
        }

        return result;
    }

    /**
     * 활성 콘서트 ID 목록 조회 (대기열이 있는 콘서트)
     */
    public Set<Long> getActiveConcertIds() {
        Set<String> keys = queueRedisTemplate.keys(WAITING_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }

        Set<Long> concertIds = new HashSet<>();
        for (String key : keys) {
            String concertIdStr = key.replace(WAITING_KEY_PREFIX, "");
            try {
                concertIds.add(Long.parseLong(concertIdStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid concert ID in key: {}", key);
            }
        }
        return concertIds;
    }

    private QueueToken mapToQueueToken(String token, Map<Object, Object> entries) {
        Long userId = Long.parseLong((String) entries.get("userId"));
        Long concertId = Long.parseLong((String) entries.get("concertId"));
        TokenStatus status = TokenStatus.valueOf((String) entries.get("status"));
        Long score = entries.get("score") != null ? Long.parseLong((String) entries.get("score")) : null;

        LocalDateTime expiresAt = null;
        if (entries.get("expiresAt") != null) {
            expiresAt = LocalDateTime.parse((String) entries.get("expiresAt"), DATE_FORMATTER);
        }

        LocalDateTime createdAt = null;
        if (entries.get("createdAt") != null) {
            createdAt = LocalDateTime.parse((String) entries.get("createdAt"), DATE_FORMATTER);
        }

        return new QueueToken(null, score, userId, concertId, token, status, expiresAt, createdAt);
    }
}
