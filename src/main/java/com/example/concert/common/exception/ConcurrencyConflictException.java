package com.example.concert.common.exception;

/**
 * 동시성 충돌 예외
 * 낙관적 락 또는 비관적 락으로 인해 동시 요청 간 충돌이 발생했을 때 사용
 */
public class ConcurrencyConflictException extends BusinessException {

    public ConcurrencyConflictException() {
        super(ErrorCode.CONCURRENCY_CONFLICT);
    }

    public ConcurrencyConflictException(String message) {
        super(ErrorCode.CONCURRENCY_CONFLICT, message);
    }
}
