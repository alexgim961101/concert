package com.example.concert.domain.point.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 포인트 도메인 객체 (순수 Java, No JPA)
 */
public class Point {
    private static final BigDecimal MAX_POINT = new BigDecimal("1000000");

    private final Long id;
    private final Long userId;
    private BigDecimal balance;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Point(Long id, Long userId, BigDecimal balance, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 Point 생성 (잔액 0원으로 초기화)
     */
    public static Point create(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return new Point(null, userId, BigDecimal.ZERO, now, now);
    }

    /**
     * 기존 Point 복원 (인프라스트럭처 계층에서 사용)
     */
    public static Point restore(Long id, Long userId, BigDecimal balance, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new Point(id, userId, balance, createdAt, updatedAt);
    }

    /**
     * 포인트 충전
     * 
     * @throws IllegalArgumentException 충전 금액이 0 이하인 경우
     * @throws IllegalStateException    충전 후 잔액이 최대 한도를 초과하는 경우
     */
    public void charge(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        BigDecimal newBalance = this.balance.add(amount);
        if (newBalance.compareTo(MAX_POINT) > 0) {
            throw new IllegalStateException("최대 충전 한도(" + MAX_POINT + "원)를 초과할 수 없습니다.");
        }

        this.balance = newBalance;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 포인트 사용
     * 
     * @throws IllegalArgumentException 사용 금액이 0 이하인 경우
     * @throws IllegalStateException    잔액이 부족한 경우
     */
    public void use(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("잔액이 부족합니다. 현재 잔액: " + this.balance + "원");
        }

        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static BigDecimal getMaxPoint() {
        return MAX_POINT;
    }
}
