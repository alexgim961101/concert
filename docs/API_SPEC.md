# API Specification

## 1. Concerts (콘서트/좌석)

### 1-1. 예약 가능 날짜 조회
- **GET** `/api/v1/concerts/{concertId}/schedules`
- **Header**: `Authorization: {QueueToken}` (Optional depending on policy)
- **Response**:
    ```json
    {
        "concertId": 1,
        "schedules": [
            {
                "id": 1, 
                "date": "2024-05-01", "availableSeats": 50 
            },
            { 
                "id": 2, 
                "date": "2024-05-02", "availableSeats": 0 
            }
        ]
    }
    ```

### 1-2. 예약 가능 좌석 조회
- **GET** `/api/v1/schedules/{scheduleId}/seats`
- **Header**: `Authorization: {QueueToken}`
- **Response**:
    ```json
    {
        "scheduleId": 1,
        "seats": [
            { 
                "id": 1, 
                "number": 1, 
                "status": "AVAILABLE", 
                "price": 10000 
            },
            { 
                "id": 2, 
                "number": 2, 
                "status": "RESERVED", 
                "price": 10000 
            }
        ]
    }
    ```

## 2. Reservation (예약)

### 2-1. 좌석 예약 요청
- **POST** `/api/v1/reservations`
- **Header**: `Authorization: {QueueToken}`
- **Body**:
    ```json
    {
        "userId": 1,
        "scheduleId": 1,
        "seatId": 1
    }
    ```
- **Response**:
    ```json
    {
        "reservationId": 123,
        "status": "PENDING",
        "expiresAt": "2024-04-01T12:05:00"
    }
    ```
    - 성공 시 좌석은 5분간 임시 배정 상태(`TEMP_RESERVED`)가 됨.

## 3. Point (포인트)

### 3-1. 포인트 충전 (충전 결제)
- **POST** `/api/v1/users/{userId}/point/charge`
- **Body**:
    ```json
    {
        "amount": 50000,
        "paymentMethod": "CARD", // CARD, TRANSFER
        "paymentDetail": {
             "cardNumber": "1234-5678-****-****"
        }
    }
    ```
- **Response**:
    ```json
    {
        "userId": 1,
        "amount": 50000,
        "totalPoint": 100000,
        "transactionId": "PG_TRANS_ID_12345",
        "chargedAt": "2024-04-01T10:00:00"
    }
    ```
    - 내부적으로 `Payment` 테이블에 `type: CHARGE`, `status: COMPLETED`로 기록됨.
    - `PointHistory`에 `type: CHARGE`, `transaction_type: PAYMENT`로 기록됨.

### 3-2. 포인트 조회
- **GET** `/api/v1/users/{userId}/point`
- **Response**:
    ```json
    {
        "userId": 1,
        "point": 100000
    }
    ```

## 4. Payment (예약 결제)

### 4-1. 예약 결제 (포인트 사용)
- **POST** `/api/v1/payments`
- **Header**: `Authorization: {QueueToken}`
- **Body**:
    ```json
    {
        "userId": 1,
        "reservationId": 123
    }
    ```
- **Response**:
    ```json
    {
        "paymentId": 999,
        "status": "COMPLETED",
        "paidAmount": 10000,
        "reservedAt": "2024-04-01T12:06:00"
    }
    ```
    - 결제 수단은 기본적으로 `POINT`로 고정됨.
    - 결제 성공 시 예약 확정(`CONFIRMED`), 좌석 확정(`RESERVED`), 대기열 토큰 만료 처리.
    - `PointHistory`에 `type: USE`, `transaction_type: PAYMENT`로 기록됨.

## 5. Queue (대기열)

### 5-1. 대기열 토큰 발급
- **POST** `/api/v1/queue/tokens`
- **Body**:
    ```json
    { 
        "userId": 1,
        "concertId": 1
    }
    ```
- **Response**:
    ```json
    {
        "token": "uuid-token-value",
        "status": "WAITING",
        "rank": 150,
        "estimatedWaitTime": 300 // seconds
    }
    ```

### 5-2. 대기열 상태(순번) 조회
- **GET** `/api/v1/queue/status`
- **Header**: `Authorization: {QueueToken}` (or pass token in query param)
- **Response**:
    ```json
    {
        "token": "uuid-token-value",
        "status": "active", // WAITING, ACTIVE, EXPIRED
        "rank": 0 // 0 if active
    }
    ```
