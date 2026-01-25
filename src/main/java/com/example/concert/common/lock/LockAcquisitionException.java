package com.example.concert.common.lock;

/**
 * 분산 락 획득 실패 시 발생하는 예외.
 */
public class LockAcquisitionException extends RuntimeException {

    private final String lockKey;

    public LockAcquisitionException(String lockKey) {
        super("Failed to acquire lock for key: " + lockKey);
        this.lockKey = lockKey;
    }

    public LockAcquisitionException(String lockKey, Throwable cause) {
        super("Failed to acquire lock for key: " + lockKey, cause);
        this.lockKey = lockKey;
    }

    public String getLockKey() {
        return lockKey;
    }
}
