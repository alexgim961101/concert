package com.example.concert.domain.reservation.entity;

/**
 * 예약 상태 열거형
 */
public enum ReservationStatus {
    PENDING, // 임시 예약 (결제 대기)
    CONFIRMED, // 예약 확정 (결제 완료)
    CANCELLED, // 예약 취소
    EXPIRED // 예약 만료 (5분 경과)
}
