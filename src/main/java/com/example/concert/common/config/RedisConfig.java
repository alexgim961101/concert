package com.example.concert.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableCaching
public class RedisConfig {

        // 캐시별 기본 TTL 설정
        private static final Duration CONCERT_SCHEDULES_TTL = Duration.ofMinutes(5);
        private static final Duration CONCERT_SCHEDULES_JITTER = Duration.ofSeconds(30);

        private static final Duration SEATS_TTL = Duration.ofSeconds(30);
        private static final Duration SEATS_JITTER = Duration.ofSeconds(5);

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                // Key Serializer
                template.setKeySerializer(new StringRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());

                // Value Serializer - JDK Serialization for record support
                JdkSerializationRedisSerializer serializer = new JdkSerializationRedisSerializer();
                template.setValueSerializer(serializer);
                template.setHashValueSerializer(serializer);

                template.afterPropertiesSet();
                return template;
        }

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                // JDK Serialization for record support
                JdkSerializationRedisSerializer serializer = new JdkSerializationRedisSerializer();

                // 기본 캐시 설정
                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                                .disableCachingNullValues();

                // 캐시별 개별 설정 (TTL + Jitter)
                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                cacheConfigurations.put("concertSchedules", defaultConfig
                                .entryTtl(withJitter(CONCERT_SCHEDULES_TTL, CONCERT_SCHEDULES_JITTER)));

                cacheConfigurations.put("seats", defaultConfig
                                .entryTtl(withJitter(SEATS_TTL, SEATS_JITTER)));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }

        /**
         * TTL에 Jitter를 추가하여 Cache Avalanche 방지
         */
        private Duration withJitter(Duration baseTtl, Duration jitterRange) {
                long jitterMillis = ThreadLocalRandom.current()
                                .nextLong(-jitterRange.toMillis(), jitterRange.toMillis());
                return baseTtl.plusMillis(jitterMillis);
        }
}
