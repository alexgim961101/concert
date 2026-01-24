-- =============================================================================
-- 성능 최적화 인덱스 마이그레이션 스크립트
-- =============================================================================

-- 1. queue_tokens: 대기열 순위 계산 쿼리 최적화
-- 쿼리: countByStatusAndIdLessThan(status, id)
-- 효과: Full Table Scan → Index Range Scan
CREATE INDEX idx_queue_tokens_status_id ON queue_tokens(status, id);

-- 2. seats: 스케줄별 좌석 조회 최적화
-- 쿼리: findByScheduleIdAndStatusIn, countByScheduleIdAndStatus
-- 효과: FK 인덱스만으로는 status 필터링 시 추가 스캔 필요 → Covering Index
CREATE INDEX idx_seats_schedule_status ON seats(schedule_id, status);

-- 3. reservations: 만료 예약 조회 최적화
-- 쿼리: findAllByStatusAndExpiresAtBefore(status, now)
-- 효과: 배치 처리 시 만료된 예약만 효율적 조회
CREATE INDEX idx_reservations_status_expires ON reservations(status, expires_at);

-- 4. payments: 사용자별 결제 조회 (향후 API 확장 대비)
CREATE INDEX idx_payments_user_id ON payments(user_id);

-- 5. payments: 예약별 결제 조회 (향후 API 확장 대비)
CREATE INDEX idx_payments_reservation_id ON payments(reservation_id);

-- 인덱스 생성 확인
SHOW INDEX FROM queue_tokens;
SHOW INDEX FROM seats;
SHOW INDEX FROM reservations;
SHOW INDEX FROM payments;
