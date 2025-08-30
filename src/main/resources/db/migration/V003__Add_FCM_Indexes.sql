-- FCM 및 알림 성능 최적화를 위한 인덱스 추가

-- User 테이블 FCM 토큰 인덱스
CREATE INDEX idx_user_fcm_token ON user(fcm_token);

-- AlertSetting 테이블 성능 최적화 인덱스
-- 목표 환율 알림 조회 최적화
CREATE INDEX idx_alert_target_price_active ON alert_setting(is_active, target_price_push, target_achieved);

-- 일일 환율 알림 조회 최적화 (시간별)
CREATE INDEX idx_alert_daily_time ON alert_setting(is_active, today_exchange_rate_push, today_exchange_rate_push_time);

-- 일일 환율 알림 중복 발송 방지 (날짜별)
CREATE INDEX idx_alert_last_daily_sent ON alert_setting(last_daily_alert_sent);

-- 사용자별 알림 설정 조회 최적화
CREATE INDEX idx_alert_user_currency ON alert_setting(user_id, currency_code, is_active);

-- FCM 토큰 중복 검색 최적화
CREATE INDEX idx_user_fcm_token_not_null ON user(fcm_token) WHERE fcm_token IS NOT NULL;

-- 복합 인덱스: 알림 설정 + 사용자 FCM 토큰 조인 최적화
CREATE INDEX idx_alert_user_fcm ON alert_setting(user_id, is_active) WHERE target_price_push = TRUE OR today_exchange_rate_push = TRUE;