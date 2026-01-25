package com.example.concert.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 추상 베이스 클래스
 * 
 * MySQL, Redis, Kafka를 모두 Testcontainers로 실행하여 테스트 격리를 보장합니다.
 * 
 * 사용법: 통합 테스트 클래스에서 이 클래스를 상속받으세요.
 * (예: class MyTest extends AbstractIntegrationTest)
 * 
 * 데이터 격리:
 * - MySQL: @Transactional 테스트는 자동 롤백, 아니면 DatabaseCleaner 사용
 * - Redis: 매 테스트 전 자동 flushAll()
 * - Kafka: 테스트별 고유 consumer group 사용 권장
 */
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> mysql;
    static final GenericContainer<?> redis;
    static final ConfluentKafkaContainer kafka;

    static {
        mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("concert")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("init.sql");
        mysql.start();

        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redis.start();

        kafka = new ConfluentKafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
        kafka.start();
    }

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 매 테스트 전 Redis 데이터 클리어
     * Redis를 사용하지 않는 테스트에서는 redisConnectionFactory가 null일 수 있음
     */
    @BeforeEach
    void cleanRedisBeforeEach() {
        if (redisConnectionFactory != null) {
            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                connection.serverCommands().flushAll();
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
