-- =============================================================================
-- Concert Reservation Service - Test Data Seed Script
-- =============================================================================
-- 인프라 테스트용 샘플 데이터를 생성합니다.
-- 실행: mysql -u <user> -p concert < seed_data.sql
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. Concerts (10개)
-- -----------------------------------------------------------------------------
INSERT INTO concerts (title, description, created_at, updated_at) VALUES
('IU 콘서트 2026', 'IU의 전국 투어 콘서트', NOW(), NOW()),
('BTS World Tour', 'BTS 월드 투어 서울 공연', NOW(), NOW()),
('Coldplay Music of the Spheres', 'Coldplay 내한 공연', NOW(), NOW()),
('뉴진스 팬미팅', 'NewJeans 첫 번째 팬미팅', NOW(), NOW()),
('에스파 SYNK 콘서트', 'aespa SYNK 콘서트', NOW(), NOW()),
('블랙핑크 Born Pink', 'BLACKPINK 월드투어 앵콜', NOW(), NOW()),
('세븐틴 Follow Tour', 'SEVENTEEN Follow Tour 서울', NOW(), NOW()),
('스트레이키즈 5-STAR', 'Stray Kids 5-STAR Dome Tour', NOW(), NOW()),
('르세라핌 FLAME RISES', 'LE SSERAFIM 첫 단독 콘서트', NOW(), NOW()),
('NCT DREAM THE DREAM SHOW', 'NCT DREAM 콘서트', NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 2. Concert Schedules (콘서트당 3회차, 총 30개)
-- -----------------------------------------------------------------------------
INSERT INTO concert_schedules (concert_id, concert_date, reservation_start_at, created_at, updated_at)
SELECT 
    c.id,
    DATE_ADD(NOW(), INTERVAL (c.id * 7 + n.num) DAY),
    DATE_SUB(DATE_ADD(NOW(), INTERVAL (c.id * 7 + n.num) DAY), INTERVAL 14 DAY),
    NOW(),
    NOW()
FROM concerts c
CROSS JOIN (SELECT 1 AS num UNION SELECT 2 UNION SELECT 3) n;

-- -----------------------------------------------------------------------------
-- 3. Seats (회차당 50석, 총 1,500개)
-- -----------------------------------------------------------------------------
INSERT INTO seats (schedule_id, seat_number, price, status, version, created_at, updated_at)
SELECT 
    cs.id,
    seat_num.num,
    CASE 
        WHEN seat_num.num <= 10 THEN 150000.00
        WHEN seat_num.num <= 30 THEN 120000.00
        ELSE 80000.00
    END,
    'AVAILABLE',
    0,
    NOW(),
    NOW()
FROM concert_schedules cs
CROSS JOIN (
    SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
    UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
    UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
    UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
    UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
    UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
) seat_num;

-- -----------------------------------------------------------------------------
-- 4. Points (유저 100명, 각 10,000 ~ 500,000 포인트)
-- -----------------------------------------------------------------------------
INSERT INTO points (user_id, balance, created_at, updated_at, version)
SELECT 
    user_num.num,
    FLOOR(10000 + RAND() * 490000),
    NOW(),
    NOW(),
    0
FROM (
    SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
    UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
    UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
    UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
    UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
    UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
    UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55
    UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
    UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65
    UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70
    UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75
    UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80
    UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85
    UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
    UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95
    UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100
) user_num;

-- -----------------------------------------------------------------------------
-- 5. Queue Tokens (대기열 토큰 200개 - 다양한 상태)
-- -----------------------------------------------------------------------------
INSERT INTO queue_tokens (user_id, concert_id, token, status, expires_at, created_at)
SELECT 
    FLOOR(1 + RAND() * 100),
    FLOOR(1 + RAND() * 10),
    UUID(),
    CASE 
        WHEN token_num.num <= 100 THEN 'WAITING'
        WHEN token_num.num <= 150 THEN 'ACTIVE'
        ELSE 'EXPIRED'
    END,
    DATE_ADD(NOW(), INTERVAL (30 - token_num.num) MINUTE),
    NOW()
FROM (
    SELECT @row := @row + 1 AS num FROM 
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t1,
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t2,
    (SELECT @row := 0) init
    LIMIT 200
) token_num;

-- -----------------------------------------------------------------------------
-- 6. Reservations (예약 50개 - 일부 좌석 예약 상태로 변경)
-- -----------------------------------------------------------------------------
-- 먼저 일부 좌석을 RESERVED 상태로 변경
UPDATE seats SET status = 'RESERVED' WHERE id <= 50;

INSERT INTO reservations (user_id, schedule_id, seat_id, status, created_at, expires_at)
SELECT 
    FLOOR(1 + RAND() * 100),
    s.schedule_id,
    s.id,
    CASE 
        WHEN s.id <= 30 THEN 'CONFIRMED'
        ELSE 'PENDING'
    END,
    NOW(),
    DATE_ADD(NOW(), INTERVAL 5 MINUTE)
FROM seats s
WHERE s.id <= 50;

-- -----------------------------------------------------------------------------
-- 7. Payments (결제 50개 - 예약과 매칭)
-- -----------------------------------------------------------------------------
INSERT INTO payments (reservation_id, user_id, amount, status, created_at)
SELECT 
    r.id,
    r.user_id,
    s.price,
    CASE 
        WHEN r.status = 'CONFIRMED' THEN 'COMPLETED'
        ELSE 'PENDING'
    END,
    NOW()
FROM reservations r
JOIN seats s ON r.seat_id = s.id;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 검증 쿼리
-- -----------------------------------------------------------------------------
SELECT '=== Data Count Summary ===' AS '';
SELECT 'concerts' AS table_name, COUNT(*) AS count FROM concerts
UNION ALL SELECT 'concert_schedules', COUNT(*) FROM concert_schedules
UNION ALL SELECT 'seats', COUNT(*) FROM seats
UNION ALL SELECT 'points', COUNT(*) FROM points
UNION ALL SELECT 'queue_tokens', COUNT(*) FROM queue_tokens
UNION ALL SELECT 'reservations', COUNT(*) FROM reservations
UNION ALL SELECT 'payments', COUNT(*) FROM payments;
