-- 은행 환전 정보 테이블 생성
CREATE TABLE bank_exchange_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(50) NOT NULL UNIQUE COMMENT '은행명',
    bank_code VARCHAR(10) NOT NULL UNIQUE COMMENT '은행 코드',
    spread_rate DECIMAL(5,2) NOT NULL COMMENT '스프레드율 (%)',
    preferential_rate DECIMAL(5,2) NOT NULL COMMENT '우대율 (%)',
    fixed_fee DECIMAL(10,2) NOT NULL COMMENT '고정 수수료',
    fee_rate DECIMAL(5,2) NOT NULL COMMENT '수수료율 (%)',
    min_amount DECIMAL(12,2) NOT NULL COMMENT '최소 환전 금액',
    max_amount DECIMAL(12,2) NOT NULL COMMENT '최대 환전 금액',
    is_online_available BOOLEAN NOT NULL DEFAULT TRUE COMMENT '온라인 환전 가능 여부',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '서비스 활성화 여부',
    description VARCHAR(200) COMMENT '부가 설명',
    display_order INT NOT NULL DEFAULT 0 COMMENT '화면 표시 순서',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시'
);

-- 인덱스 생성
CREATE INDEX idx_bank_exchange_info_active ON bank_exchange_info(is_active);
CREATE INDEX idx_bank_exchange_info_online ON bank_exchange_info(is_online_available);
CREATE INDEX idx_bank_exchange_info_display_order ON bank_exchange_info(display_order);
CREATE INDEX idx_bank_exchange_info_bank_name ON bank_exchange_info(bank_name);
CREATE INDEX idx_bank_exchange_info_bank_code ON bank_exchange_info(bank_code);