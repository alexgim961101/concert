package com.example.concert.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 분산 락을 적용할 메서드에 사용하는 어노테이션.
 * AOP를 통해 메서드 실행 전후로 Redis 락을 획득/해제합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * 락 키 (SpEL 표현식 지원)
     * 예: "'seat:' + #seatId"
     */
    String key();

    /**
     * 락 획득 대기 시간 (기본: 5초)
     */
    long waitTime() default 5L;

    /**
     * 락 점유 시간 (기본: 10초)
     * 이 시간이 지나면 락이 자동 해제됩니다.
     */
    long leaseTime() default 10L;

    /**
     * 시간 단위 (기본: 초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
