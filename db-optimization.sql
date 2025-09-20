-- 📊 DB 인덱스 최적화 스크립트
-- 환율 앱 성능 향상을 위한 복합 인덱스 및 최적화 쿼리

-- ========================================
-- 1. 알림 설정 테이블 최적화
-- ========================================

-- 목표 환율 알림 조회 최적화 (스케줄러용)
CREATE INDEX idx_alert_target_active 
ON alert_settings(is_active, target_price_push, currency_code, target_achieved) 
WHERE is_active = 1 AND target_price_push = 1 AND target_achieved = 0;

-- 일일 환율 알림 조회 최적화 (스케줄러용)  
CREATE INDEX idx_alert_daily_active
ON alert_settings(is_active, today_exchange_rate_push, today_exchange_rate_push_time)
WHERE is_active = 1 AND today_exchange_rate_push = 1;

-- 사용자별 알림 설정 조회 최적화
CREATE INDEX idx_alert_user_currency 
ON alert_settings(user_id, currency_code, is_active);

-- ========================================
-- 2. 환율 데이터 테이블 최적화  
-- ========================================

-- 환율 히스토리 기간별 조회 최적화
CREATE INDEX idx_exchange_history_period
ON exchange_rate_history(currency_code, base_date DESC);

-- 최신 환율 조회 최적화 (created_at 기준)
CREATE INDEX idx_exchange_rates_latest
ON exchange_rates(currency_code, created_at DESC);

-- ========================================
-- 3. 사용자 테이블 최적화
-- ========================================

-- FCM 토큰 조회 최적화 (알림 발송용)
CREATE INDEX idx_users_fcm_active
ON users(fcm_token, is_deleted)
WHERE fcm_token IS NOT NULL AND is_deleted = 0;

-- Apple 사용자 조회 최적화
CREATE INDEX idx_users_apple_provider
ON users(provider, provider_id)  
WHERE provider = 'APPLE';

-- 탈퇴 예정 사용자 조회 최적화 (배치 작업용)
CREATE INDEX idx_users_final_deletion
ON users(final_deletion_date, is_deleted)
WHERE is_deleted = 1 AND final_deletion_date IS NOT NULL;

-- ========================================
-- 4. 은행 환전 정보 최적화
-- ========================================

-- 활성 은행 정보 조회 최적화
CREATE INDEX idx_bank_info_active
ON bank_exchange_info(is_active, display_order)
WHERE is_active = 1;

-- ========================================
-- 5. 복합 쿼리 최적화
-- ========================================

-- 사용자 + 알림 설정 조인 최적화
-- (AlertSettingServiceImpl에서 자주 사용되는 쿼리)
CREATE INDEX idx_users_alerts_join
ON alert_settings(user_id, is_active, target_price_push, today_exchange_rate_push);

-- ========================================
-- 6. 기존 인덱스 검토 및 개선
-- ========================================

-- 중복 인덱스 제거 (기존 단일 컬럼 인덱스가 복합 인덱스에 포함된 경우)
-- 실행 전 기존 인덱스 확인 필요:
-- SHOW INDEX FROM alert_settings;
-- SHOW INDEX FROM exchange_rates;
-- SHOW INDEX FROM users;

-- ========================================
-- 7. 통계 정보 업데이트
-- ========================================

-- 테이블 통계 정보 갱신 (MySQL 8.0+)
ANALYZE TABLE alert_settings;
ANALYZE TABLE exchange_rates; 
ANALYZE TABLE exchange_rate_history;
ANALYZE TABLE users;
ANALYZE TABLE bank_exchange_info;

-- ========================================
-- 8. 성능 모니터링 쿼리
-- ========================================

-- 느린 쿼리 확인
-- SELECT * FROM performance_schema.events_statements_summary_by_digest 
-- WHERE avg_timer_wait > 1000000000 -- 1초 이상
-- ORDER BY avg_timer_wait DESC LIMIT 10;

-- 인덱스 사용률 확인  
-- SELECT object_schema, object_name, index_name, count_read, count_write
-- FROM performance_schema.table_io_waits_summary_by_index_usage
-- WHERE object_schema = 'your_database_name'
-- ORDER BY count_read DESC;

-- ========================================
-- 📝 적용 순서 및 주의사항
-- ========================================

/*
1. 운영 환경 적용 전 개발/스테이징에서 충분한 테스트
2. 인덱스 생성은 부하가 적은 시간대에 실행 
3. 기존 인덱스와 중복되지 않는지 확인
4. 인덱스 생성 후 쿼리 성능 측정 및 비교
5. 불필요한 인덱스는 제거하여 INSERT/UPDATE 성능 확보

예상 성능 향상:
- 알림 스케줄러: 50-80% 성능 향상
- 환율 조회 API: 30-50% 응답시간 단축  
- 사용자 관련 쿼리: 40-60% 성능 향상
*/