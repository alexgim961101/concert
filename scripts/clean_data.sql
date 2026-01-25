-- =============================================================================
-- Concert Reservation Service - Test Data Clean Script
-- =============================================================================
-- 모든 테스트 데이터를 삭제합니다.
-- 실행: mysql -u <user> -p concert < clean_data.sql
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- FK 역순으로 삭제
TRUNCATE TABLE payments;
TRUNCATE TABLE reservations;
TRUNCATE TABLE queue_tokens;
TRUNCATE TABLE seats;
TRUNCATE TABLE concert_schedules;
TRUNCATE TABLE concerts;
TRUNCATE TABLE points;

SET FOREIGN_KEY_CHECKS = 1;

-- 검증
SELECT '=== All tables cleaned ===' AS '';
SELECT 'concerts' AS table_name, COUNT(*) AS count FROM concerts
UNION ALL SELECT 'concert_schedules', COUNT(*) FROM concert_schedules
UNION ALL SELECT 'seats', COUNT(*) FROM seats
UNION ALL SELECT 'points', COUNT(*) FROM points
UNION ALL SELECT 'queue_tokens', COUNT(*) FROM queue_tokens
UNION ALL SELECT 'reservations', COUNT(*) FROM reservations
UNION ALL SELECT 'payments', COUNT(*) FROM payments;
