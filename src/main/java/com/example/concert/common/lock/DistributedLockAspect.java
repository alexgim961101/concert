package com.example.concert.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @DistributedLock 어노테이션을 처리하는 AOP Aspect.
 * 
 *                  트랜잭션보다 먼저 실행되어야 하므로 Order를 낮은 값으로 설정합니다.
 *                  (Order 값이 낮을수록 먼저 실행됨)
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1) // @Transactional 보다 먼저 실행
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private static final String LOCK_KEY_PREFIX = "lock:";

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = LOCK_KEY_PREFIX + parseKey(joinPoint, distributedLock.key());
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit());

            if (!acquired) {
                log.warn("Failed to acquire lock: {}", lockKey);
                throw new LockAcquisitionException(lockKey);
            }

            log.debug("Lock acquired: {}", lockKey);
            return joinPoint.proceed();

        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", lockKey);
            }
        }
    }

    /**
     * SpEL 표현식을 파싱하여 실제 락 키를 생성합니다.
     */
    private String parseKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        StandardEvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < parameters.length; i++) {
            context.setVariable(parameters[i].getName(), args[i]);
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
