# 프로젝트 초기 세팅 가이드

## 개요
이 문서는 Concert 프로젝트의 초기 인프라 세팅을 위한 가이드입니다.
- MySQL: 관계형 데이터베이스
- Redis: 캐시 및 세션 저장소
- Kafka: 메시지 브로커
- Testcontainers: 통합 테스트용 컨테이너
- JPA: ORM 프레임워크

---

## 1. Gradle 의존성 추가 (`build.gradle`)

### 필수 의존성

#### JPA & MySQL
```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
runtimeOnly 'com.mysql:mysql-connector-j'
```

#### Redis
```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

#### Kafka
```gradle
implementation 'org.springframework.kafka:spring-kafka'
```

#### Testcontainers
```gradle
testImplementation platform('org.testcontainers:testcontainers-bom:1.19.3')
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:mysql'
testImplementation 'org.testcontainers:kafka'
```

### 전체 의존성 예시
```gradle
dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.kafka:spring-kafka'
    
    // Database
    runtimeOnly 'com.mysql:mysql-connector-j'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation platform('org.testcontainers:testcontainers-bom:1.19.3')
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:mysql'
    testImplementation 'org.testcontainers:kafka'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

---

## 2. Application 설정 (`application.yml`)

### 프로파일 구조
- `application.yml` - 공통 설정
- `application-local.yml` - 로컬 개발 환경
- `application-test.yml` - 테스트 환경 (Testcontainers 사용)

### 2.1 공통 설정 (`application.yml`)

```yaml
spring:
  application:
    name: concert
  
  profiles:
    active: local
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect
    open-in-view: false
  
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
    consumer:
      group-id: concert-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
```

### 2.2 로컬 환경 설정 (`application-local.yml`)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/concert?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
  
  data:
    redis:
      host: localhost
      port: 6379
  
  kafka:
    bootstrap-servers: localhost:9092
```

### 2.3 테스트 환경 설정 (`application-test.yml`)

```yaml
spring:
  datasource:
    url: jdbc:tc:mysql:8.0:///concert?TC_INITSCRIPT=init.sql
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  
  data:
    redis:
      host: localhost
      port: 6379
  
  kafka:
    bootstrap-servers: localhost:9092
```

---

## 3. JPA 설정

### 3.1 JPA Auditing 활성화

`ConcertApplication.java`에 어노테이션 추가:
```java
@EnableJpaAuditing
@SpringBootApplication
public class ConcertApplication {
    // ...
}
```

### 3.2 Base Entity 클래스

```java
package com.example.concert.common.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### 3.3 Repository 구조

```java
package com.example.concert.domain.user.repository;

import com.example.concert.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // 커스텀 쿼리 메서드
    // 예: Optional<User> findByEmail(String email);
}
```

---

## 4. Redis 설정 클래스

### 4.1 RedisConfig.java

```java
package com.example.concert.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key Serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value Serializer
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
```

---

## 5. Kafka 설정 클래스

### 5.1 KafkaConfig.java

```java
package com.example.concert.common.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "concert-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
```

---

## 6. 전역 예외 처리 및 공통 DTO 설정

### 6.1 공통 응답 DTO

#### ApiResponse.java
```java
package com.example.concert.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, null, message, null);
    }

    public static <T> ApiResponse<T> error(String message, String code) {
        return new ApiResponse<>(false, null, null, new ErrorResponse(code, message));
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, null, error);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
    }
}
```

### 6.2 공통 요청 DTO

#### PageRequest.java
```java
package com.example.concert.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class PageRequest {
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private int size = 10;

    private String sortBy = "id";
    private Sort.Direction direction = Sort.Direction.DESC;

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
```

### 6.3 에러 코드 Enum

#### ErrorCode.java
```java
package com.example.concert.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통 에러
    INTERNAL_SERVER_ERROR("E0001", "내부 서버 오류가 발생했습니다"),
    INVALID_INPUT_VALUE("E0002", "잘못된 입력 값입니다"),
    METHOD_NOT_ALLOWED("E0003", "허용되지 않은 HTTP 메서드입니다"),
    
    // 인증/인가 에러
    UNAUTHORIZED("E1001", "인증이 필요합니다"),
    FORBIDDEN("E1002", "접근 권한이 없습니다"),
    
    // 리소스 에러
    RESOURCE_NOT_FOUND("E2001", "요청한 리소스를 찾을 수 없습니다"),
    RESOURCE_ALREADY_EXISTS("E2002", "이미 존재하는 리소스입니다"),
    
    // 비즈니스 로직 에러
    BUSINESS_LOGIC_ERROR("E3001", "비즈니스 로직 오류가 발생했습니다"),
    
    // 데이터베이스 에러
    DATA_ACCESS_ERROR("E4001", "데이터베이스 오류가 발생했습니다"),
    
    // 외부 서비스 에러
    EXTERNAL_SERVICE_ERROR("E5001", "외부 서비스 오류가 발생했습니다");

    private final String code;
    private final String message;
}
```

### 6.4 커스텀 예외 클래스

#### BusinessException.java
```java
package com.example.concert.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

#### ResourceNotFoundException.java
```java
package com.example.concert.common.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
```

### 6.5 전역 예외 처리 핸들러

#### GlobalExceptionHandler.java
```java
package com.example.concert.common.exception;

import com.example.concert.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            errorCode.getCode(),
            e.getMessage()
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * 리소스 없음 예외 처리
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("ResourceNotFoundException: {}", e.getMessage());
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            ErrorCode.RESOURCE_NOT_FOUND.getCode(),
            e.getMessage()
        );
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(error));
    }

    /**
     * Validation 예외 처리 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            ErrorCode.INVALID_INPUT_VALUE.getCode(),
            "입력 값 검증에 실패했습니다: " + errors
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * Bind 예외 처리 (@ModelAttribute)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        log.warn("BindException: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            ErrorCode.INVALID_INPUT_VALUE.getCode(),
            "입력 값 검증에 실패했습니다: " + errors
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            ErrorCode.INVALID_INPUT_VALUE.getCode(),
            String.format("'%s'의 값 '%s'이(가) 올바르지 않습니다", e.getName(), e.getValue())
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * HTTP 메시지 읽기 예외 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            ErrorCode.INVALID_INPUT_VALUE.getCode(),
            "요청 본문을 읽을 수 없습니다"
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * HTTP 메서드 미지원 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            ErrorCode.METHOD_NOT_ALLOWED.getCode(),
            "지원하지 않는 HTTP 메서드입니다: " + e.getMethod()
        );
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error(error));
    }

    /**
     * 데이터베이스 접근 예외 처리
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessException(DataAccessException e) {
        log.error("DataAccessException: ", e);
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            ErrorCode.DATA_ACCESS_ERROR.getCode(),
            "데이터베이스 오류가 발생했습니다"
        );
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(error));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            ErrorCode.INVALID_INPUT_VALUE.getCode(),
            e.getMessage()
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("Unexpected exception: ", e);
        ApiResponse.ErrorResponse error = new ApiResponse.ErrorResponse(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            "예상치 못한 오류가 발생했습니다"
        );
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(error));
    }
}
```

### 6.6 Validation 의존성 추가

`build.gradle`에 Validation 의존성 추가:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

---

## 7. Testcontainers 설정

### 7.1 통합 테스트 예시

```java
package com.example.concert.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class IntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("concert")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest"));
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @BeforeEach
    void setUp() {
        // 테스트 전 초기화 작업
    }
}
```

---

## 8. 프로젝트 디렉토리 구조

### 8.1 클린 아키텍처 + 레이어드 아키텍처 구조

프로젝트는 **클린 아키텍처**와 **레이어드 아키텍처**를 결합한 구조로 구성됩니다:

**클린 아키텍처 원칙:**
- **의존성 규칙**: 내부 레이어는 외부 레이어를 알 수 없음 (의존성은 항상 안쪽으로)
- **레이어 분리**: Domain → Application → Infrastructure → Presentation 순서로 의존성 방향

**구조:**
- **common**: 공통 설정, 유틸리티, 공통 DTO 등
- **domain**: 도메인별 모듈 (각 도메인은 클린 아키텍처 레이어 구조)

```
src/main/java/com/example/concert/
├── common/                      # 공통 모듈
│   ├── config/                  # 설정 클래스들
│   │   ├── RedisConfig.java
│   │   ├── KafkaConfig.java
│   │   ├── JacksonConfig.java
│   │   ├── WebConfig.java
│   │   └── AsyncConfig.java
│   ├── dto/                     # 공통 DTO
│   │   ├── ApiResponse.java
│   │   └── PageRequest.java
│   ├── exception/               # 예외 처리
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ErrorCode.java
│   │   ├── BusinessException.java
│   │   └── ResourceNotFoundException.java
│   ├── domain/                  # 공통 도메인 (BaseEntity 등)
│   │   └── BaseEntity.java
│   └── util/                    # 공통 유틸리티 (선택)
│       └── ...
│
├── domain/                      # 도메인별 모듈
│   ├── user/                    # User 도메인 예시
│   │   ├── domain/              # Domain Layer (가장 내부, 순수 비즈니스 로직)
│   │   │   ├── entity/          # 엔티티
│   │   │   │   └── User.java
│   │   │   ├── vo/              # Value Object
│   │   │   │   └── Email.java
│   │   │   ├── service/         # Domain Service (순수 비즈니스 로직)
│   │   │   │   └── UserDomainService.java
│   │   │   └── repository/      # Repository Interface (포트)
│   │   │       └── UserRepository.java
│   │   │
│   │   ├── application/         # Application Layer (Use Case)
│   │   │   ├── port/            # 포트 (인터페이스)
│   │   │   │   ├── in/          # 입력 포트 (Use Case 인터페이스)
│   │   │   │   │   └── UserUseCase.java
│   │   │   │   └── out/         # 출력 포트 (Repository Interface)
│   │   │   │       └── UserRepositoryPort.java
│   │   │   └── service/         # Use Case 구현
│   │   │       └── UserService.java
│   │   │
│   │   ├── infrastructure/      # Infrastructure Layer (어댑터)
│   │   │   └── persistence/    # Repository 구현체
│   │   │       └── UserJpaRepository.java
│   │   │
│   │   └── presentation/        # Presentation Layer (Controller, DTO)
│   │       ├── controller/      # Controller
│   │       │   └── UserController.java
│   │       └── dto/             # DTO
│   │           ├── request/
│   │           │   ├── UserCreateRequest.java
│   │           │   └── UserUpdateRequest.java
│   │           └── response/
│   │               └── UserResponse.java
│   │
│   ├── concert/                 # Concert 도메인 예시
│   │   ├── domain/
│   │   │   ├── entity/
│   │   │   ├── vo/
│   │   │   ├── service/
│   │   │   └── repository/
│   │   ├── application/
│   │   │   ├── port/
│   │   │   └── service/
│   │   ├── infrastructure/
│   │   │   └── persistence/
│   │   └── presentation/
│   │       ├── controller/
│   │       └── dto/
│   │
│   └── {other-domain}/          # 기타 도메인들
│       ├── domain/
│       ├── application/
│       ├── infrastructure/
│       └── presentation/
│
└── ConcertApplication.java

src/test/java/com/example/concert/
├── common/                      # 공통 테스트 유틸리티
│   └── TestUtil.java
├── domain/                      # 도메인별 테스트
│   ├── user/
│   │   ├── domain/              # Domain Layer 테스트
│   │   │   ├── entity/
│   │   │   ├── vo/
│   │   │   └── service/
│   │   ├── application/         # Application Layer 테스트
│   │   │   └── service/
│   │   ├── infrastructure/      # Infrastructure Layer 테스트
│   │   │   └── persistence/
│   │   └── presentation/        # Presentation Layer 테스트
│   │       └── controller/
│   └── concert/
│       ├── domain/
│       ├── application/
│       ├── infrastructure/
│       └── presentation/
└── integration/                 # 통합 테스트
    └── IntegrationTest.java
```

### 8.2 클린 아키텍처 레이어별 역할

#### Domain Layer (domain) - 가장 내부 레이어
**책임:**
- 핵심 비즈니스 로직 (순수 비즈니스 규칙)
- 엔티티 (Entity) 정의
- 값 객체 (Value Object) 정의
- 도메인 서비스 (복잡한 비즈니스 로직)
- Repository 인터페이스 정의 (포트)

**특징:**
- 외부 레이어에 의존하지 않음 (순수 Java 코드)
- 프레임워크에 의존하지 않음 (JPA 어노테이션은 허용)
- 비즈니스 규칙의 진실의 원천 (Single Source of Truth)

**구조:**
```
domain/
├── entity/          # 엔티티 (JPA Entity)
├── vo/              # Value Object (불변 객체)
├── service/         # Domain Service (순수 비즈니스 로직)
└── repository/      # Repository Interface (포트)
```

#### Application Layer (application) - Use Case Layer
**책임:**
- Use Case 구현 (애플리케이션 비즈니스 로직)
- 트랜잭션 관리
- 도메인 객체 조합 및 조율
- 입력/출력 포트 정의

**특징:**
- Domain Layer에만 의존
- Infrastructure Layer의 구현체를 직접 참조하지 않음 (포트를 통해)
- Use Case는 하나의 비즈니스 작업 단위

**구조:**
```
application/
├── port/
│   ├── in/          # 입력 포트 (Use Case 인터페이스)
│   └── out/         # 출력 포트 (Repository Interface)
└── service/         # Use Case 구현체
```

#### Infrastructure Layer (infrastructure) - 어댑터 Layer
**책임:**
- Repository 구현체 (JPA Repository 어댑터)
- 외부 시스템 연동 (Kafka, Redis 등)
- 데이터 영속성 처리

**특징:**
- Application Layer의 포트를 구현
- 프레임워크에 의존 (Spring, JPA 등)
- 외부 시스템과의 통신 담당

**구조:**
```
infrastructure/
└── persistence/     # Repository 구현체 (어댑터)
```

#### Presentation Layer (presentation) - 인터페이스 Layer
**책임:**
- HTTP 요청/응답 처리
- 요청 검증
- DTO 변환
- Application Layer의 Use Case 호출

**특징:**
- Application Layer에만 의존
- Domain Layer를 직접 참조하지 않음
- 사용자 인터페이스와의 통신 담당

**구조:**
```
presentation/
├── controller/      # REST Controller
└── dto/             # Data Transfer Object
    ├── request/
    └── response/
```

### 8.3 의존성 규칙 (Dependency Rule)

클린 아키텍처의 핵심 원칙:

```
Presentation → Application → Domain ← Infrastructure
     ↓              ↓           ↑            ↑
     └──────────────┴───────────┴────────────┘
```

**규칙:**
1. **의존성은 항상 안쪽으로**: 외부 레이어는 내부 레이어에 의존
2. **내부 레이어는 외부 레이어를 알 수 없음**: Domain은 Application, Infrastructure, Presentation을 알 수 없음
3. **포트와 어댑터 패턴**: Application은 Infrastructure의 구현체가 아닌 포트(인터페이스)에 의존
4. **역전 의존성**: Infrastructure는 Application의 포트를 구현하여 의존성 역전

### 8.4 도메인별 DTO 구조

각 도메인의 Presentation Layer에 DTO가 위치:

```
domain/{domain-name}/presentation/dto/
├── request/              # 요청 DTO
│   ├── {Domain}CreateRequest.java
│   ├── {Domain}UpdateRequest.java
│   └── {Domain}SearchRequest.java
└── response/             # 응답 DTO
    ├── {Domain}Response.java
    └── {Domain}DetailResponse.java
```

### 8.5 공통 모듈 (common) 구조

```
common/
├── config/               # 전역 설정 클래스
├── dto/                  # 공통 응답/요청 DTO
├── exception/            # 전역 예외 처리
├── domain/               # 공통 도메인 (BaseEntity 등)
└── util/                 # 공통 유틸리티 (선택)
```

### 8.6 패키지 네이밍 규칙

- **도메인**: 소문자, 단수형 (예: `user`, `concert`, `ticket`)
- **레이어**: 소문자 (예: `domain`, `application`, `infrastructure`, `presentation`)
- **클래스**: PascalCase (예: `UserController`, `UserService`, `UserUseCase`)
- **DTO**: `{Domain}{Purpose}Request/Response` 또는 `{Purpose}Command` (예: `CreateUserCommand`, `UserResponse`)
- **포트**: `{Domain}UseCase` (입력 포트), `{Domain}RepositoryPort` (출력 포트)

### 8.7 예시: User 도메인 구조 (클린 아키텍처)

```java
// ============================================
// Domain Layer (가장 내부)
// ============================================

// domain/user/domain/entity/User.java
package com.example.concert.domain.user.domain.entity;

import com.example.concert.common.domain.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @Embedded
    private Email email;
    
    // 생성자
    protected User() {} // JPA용
    
    public User(String name, Email email) {
        this.name = name;
        this.email = email;
    }
    
    // 비즈니스 로직 메서드
    public void changeName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.name = newName;
    }
    
    public void changeEmail(Email newEmail) {
        if (newEmail == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        this.email = newEmail;
    }
    
    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public Email getEmail() { return email; }
}

// domain/user/domain/vo/Email.java
package com.example.concert.domain.user.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Embeddable
public class Email {
    private String value;
    
    public Email(String value) {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.value = value;
    }
}

// domain/user/domain/repository/UserRepository.java (포트)
package com.example.concert.domain.user.domain.repository;

import com.example.concert.domain.user.domain.entity.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    void delete(User user);
}

// ============================================
// Application Layer (Use Case)
// ============================================

// domain/user/application/port/in/UserUseCase.java (입력 포트)
package com.example.concert.domain.user.application.port.in;

import com.example.concert.domain.user.presentation.dto.response.UserResponse;

public interface UserUseCase {
    UserResponse createUser(CreateUserCommand command);
    UserResponse getUser(Long id);
}

// domain/user/application/port/out/UserRepositoryPort.java (출력 포트)
package com.example.concert.domain.user.application.port.out;

import com.example.concert.domain.user.domain.entity.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
}

// domain/user/application/service/UserService.java (Use Case 구현)
package com.example.concert.domain.user.application.service;

import com.example.concert.common.exception.ResourceNotFoundException;
import com.example.concert.domain.user.application.port.in.UserUseCase;
import com.example.concert.domain.user.application.port.out.UserRepositoryPort;
import com.example.concert.domain.user.domain.entity.User;
import com.example.concert.domain.user.domain.vo.Email;
import com.example.concert.domain.user.presentation.dto.request.CreateUserCommand;
import com.example.concert.domain.user.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    
    @Override
    public UserResponse createUser(CreateUserCommand command) {
        // 비즈니스 로직
        Email email = new Email(command.getEmail());
        User user = new User(command.getName(), email);
        
        User savedUser = userRepositoryPort.save(user);
        return UserResponse.from(savedUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepositoryPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return UserResponse.from(user);
    }
}

// ============================================
// Infrastructure Layer (어댑터)
// ============================================

// domain/user/infrastructure/persistence/UserJpaRepository.java
package com.example.concert.domain.user.infrastructure.persistence;

import com.example.concert.domain.user.application.port.out.UserRepositoryPort;
import com.example.concert.domain.user.domain.entity.User;
import com.example.concert.domain.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserJpaRepository implements UserRepositoryPort {
    private final UserJpaRepositoryAdapter userJpaRepositoryAdapter;
    
    @Override
    public User save(User user) {
        return userJpaRepositoryAdapter.save(user);
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepositoryAdapter.findById(id);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepositoryAdapter.findByEmail(email);
    }
}

// domain/user/infrastructure/persistence/UserJpaRepositoryAdapter.java
package com.example.concert.domain.user.infrastructure.persistence;

import com.example.concert.domain.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepositoryAdapter extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email.value = :email")
    Optional<User> findByEmail(@Param("email") String email);
}

// ============================================
// Presentation Layer (Controller, DTO)
// ============================================

// domain/user/presentation/dto/request/CreateUserCommand.java
package com.example.concert.domain.user.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CreateUserCommand {
    @NotBlank
    private String name;
    
    @NotBlank
    @Email
    private String email;
}

// domain/user/presentation/dto/response/UserResponse.java
package com.example.concert.domain.user.presentation.dto.response;

import com.example.concert.domain.user.domain.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    
    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.name = user.getName();
        response.email = user.getEmail().getValue();
        return response;
    }
}

// domain/user/presentation/controller/UserController.java
package com.example.concert.domain.user.presentation.controller;

import com.example.concert.common.dto.ApiResponse;
import com.example.concert.domain.user.application.port.in.UserUseCase;
import com.example.concert.domain.user.presentation.dto.request.CreateUserCommand;
import com.example.concert.domain.user.presentation.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase userUseCase;
    
    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserCommand command) {
        return ApiResponse.success(userUseCase.createUser(command));
    }
    
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(userUseCase.getUser(id));
    }
}
```

---

## 9. 추가 고려사항

### 9.1 로깅 설정
- Logback 설정 파일 (`logback-spring.xml`) 추가 고려
- 프로파일별 로그 레벨 설정
- 자세한 예시는 10.6 섹션 참고

### 9.2 Validation
```gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

### 9.3 모니터링 (Actuator)
```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

### 9.4 환경 변수 관리
- 민감한 정보(비밀번호 등)는 환경 변수나 시크릿 관리 도구 사용
- `.env` 파일 사용 시 주의 (Git에 커밋하지 않기)

### 9.5 Docker Compose (로컬 개발용)
로컬 개발 환경을 위한 `docker-compose.yml` 파일 생성:

프로젝트 루트에 `docker-compose.yml` 파일 생성:
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: concert-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: concert
      MYSQL_USER: concert_user
      MYSQL_PASSWORD: concert_password
      TZ: Asia/Seoul
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: concert-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    container_name: concert-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: concert-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mysql_data:
  redis_data:
```

사용법:
```bash
# 모든 서비스 시작
docker-compose up -d

# 특정 서비스만 시작
docker-compose up -d mysql redis

# 서비스 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f kafka

# 모든 서비스 중지 및 삭제
docker-compose down

# 볼륨까지 삭제
docker-compose down -v
```

---

## 10. 추가 설정 (선택사항)

### 10.1 Jackson 설정 (JSON 직렬화/역직렬화)

#### JacksonConfig.java
```java
package com.example.concert.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {
    
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        
        javaTimeModule.addSerializer(LocalDateTime.class, 
            new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, 
            new LocalDateTimeDeserializer(dateTimeFormatter));
        
        return builder
            .modules(javaTimeModule)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    }
}
```

또는 `application.yml`에 설정:
```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: Asia/Seoul
    date-format: yyyy-MM-dd HH:mm:ss
```

### 10.2 CORS 설정

#### WebConfig.java
```java
package com.example.concert.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "http://localhost:8080")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

### 10.3 트랜잭션 관리

#### 트랜잭션 설정 예시
```java
package com.example.concert.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)  // 기본적으로 읽기 전용
public class UserService {
    
    @Transactional  // 쓰기 작업은 명시적으로 트랜잭션 설정
    public void createUser(UserCreateRequest request) {
        // ...
    }
    
    // 읽기 작업은 클래스 레벨 @Transactional(readOnly = true) 적용
    public UserResponse getUser(Long id) {
        // ...
    }
}
```

### 10.4 캐싱 설정

#### CacheConfig.java
```java
package com.example.concert.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("yourCacheName");
    }
}
```

사용 예시:
```java
package com.example.concert.domain.user.service;

@Service
public class UserService {
    
    @Cacheable(value = "userCache", key = "#id")
    public UserResponse getUser(Long id) {
        // ...
    }
    
    @CacheEvict(value = "userCache", key = "#id")
    public void deleteUser(Long id) {
        // ...
    }
}
```

### 10.5 비동기 처리 설정

#### AsyncConfig.java
```java
package com.example.concert.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

사용 예시:
```java
package com.example.concert.domain.user.service;

import java.util.concurrent.CompletableFuture;

@Service
public class UserService {
    
    @Async("taskExecutor")
    public CompletableFuture<String> sendEmailAsync(Long userId) {
        // 비동기 작업 (이메일 발송 등)
        return CompletableFuture.completedFuture("Email sent");
    }
}
```

### 10.6 로깅 설정 예시

#### logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <springProfile name="local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
    
    <springProfile name="!local">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/concert.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/concert-%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

### 10.7 API 문서화 (Swagger/OpenAPI)

의존성 추가:
```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
```

#### SwaggerConfig.java
```java
package com.example.concert.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Concert API")
                .description("Concert 프로젝트 API 문서")
                .version("v1.0"));
    }
}
```

접속 URL: `http://localhost:8080/swagger-ui/index.html`

---

## 11. 초기 세팅 순서 가이드

### 1단계: 의존성 추가
1. `build.gradle`에 필요한 의존성 추가
2. Gradle 동기화

### 2단계: 인프라 실행 (Docker Compose 사용 권장)
```bash
docker-compose up -d
```

### 3단계: 설정 파일 생성
1. `application.yml` 생성
2. `application-local.yml` 생성
3. `application-test.yml` 생성

### 4단계: 공통 모듈 생성 (common)
1. `common/domain/BaseEntity` 생성
2. `common/dto/ApiResponse` 생성
3. `common/dto/PageRequest` 생성
4. `common/exception/ErrorCode` Enum 생성
5. `common/exception/BusinessException` 생성
6. `common/exception/GlobalExceptionHandler` 생성

### 5단계: 공통 설정 클래스 생성 (common/config)
1. `common/config/RedisConfig` 생성
2. `common/config/KafkaConfig` 생성
3. (선택) `common/config/JacksonConfig` 생성
4. (선택) `common/config/WebConfig` 생성

### 6단계: JPA Auditing 활성화
- `ConcertApplication.java`에 `@EnableJpaAuditing` 추가

### 7단계: 테스트 설정
1. `IntegrationTest` 클래스 생성
2. 통합 테스트 실행 확인

### 8단계: 애플리케이션 실행 확인
```bash
./gradlew bootRun
```

---

## 12. API 응답 예시

### 성공 응답 예시
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "콘서트명",
    "createdAt": "2024-01-01 10:00:00"
  },
  "message": null,
  "error": null
}
```

### 에러 응답 예시
```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": {
    "code": "E2001",
    "message": "요청한 리소스를 찾을 수 없습니다"
  }
}
```

### 페이징 응답 예시
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10
  },
  "message": null,
  "error": null
}
```

---

## 13. 체크리스트

### 필수 항목
- [ ] Gradle 의존성 추가 완료
- [ ] `application.yml` 설정 완료
- [ ] `application-local.yml` 설정 완료
- [ ] `application-test.yml` 설정 완료
- [ ] JPA Auditing 활성화
- [ ] 공통 모듈 (common) 구조 생성
  - [ ] `common/domain/BaseEntity` 생성
  - [ ] `common/dto/ApiResponse` 생성
  - [ ] `common/dto/PageRequest` 생성
  - [ ] `common/exception/ErrorCode` Enum 생성
  - [ ] `common/exception/BusinessException` 생성
  - [ ] `common/exception/GlobalExceptionHandler` 생성
- [ ] 공통 설정 클래스 생성
  - [ ] `common/config/RedisConfig` 생성
  - [ ] `common/config/KafkaConfig` 생성
- [ ] Testcontainers 통합 테스트 설정
- [ ] Validation 의존성 추가
- [ ] 도메인별 디렉토리 구조 생성 (예: `domain/user/`, `domain/concert/`)
- [ ] 로컬 환경에서 MySQL, Redis, Kafka 실행 확인
- [ ] 통합 테스트 실행 확인

### 선택 항목
- [ ] Jackson 설정 (날짜 형식 등)
- [ ] CORS 설정
- [ ] 로깅 설정 (logback-spring.xml)
- [ ] 트랜잭션 관리 설정
- [ ] 캐싱 설정
- [ ] 비동기 처리 설정
- [ ] API 문서화 (Swagger/OpenAPI)
- [ ] Docker Compose 설정

---

## 참고 자료

### 공식 문서
- [Spring Data JPA 공식 문서](https://spring.io/projects/spring-data-jpa)
- [Spring Data Redis 공식 문서](https://spring.io/projects/spring-data-redis)
- [Spring for Apache Kafka 공식 문서](https://spring.io/projects/spring-kafka)
- [Testcontainers 공식 문서](https://www.testcontainers.org/)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)

### 유용한 링크
- [Jackson 직렬화 가이드](https://www.baeldung.com/jackson)
- [Spring CORS 설정](https://www.baeldung.com/spring-cors)
- [Spring 트랜잭션 관리](https://www.baeldung.com/transaction-configuration-with-jpa-and-spring)
- [Spring Cache 사용법](https://www.baeldung.com/spring-cache-tutorial)