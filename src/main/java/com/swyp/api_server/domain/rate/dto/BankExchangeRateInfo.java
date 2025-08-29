package com.swyp.api_server.domain.rate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 은행별 환율 정보
 * - 기본 환율, 우대율, 수수료 정보 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankExchangeRateInfo {
    
    private String bankName;                    // 은행명
    private String bankCode;                    // 은행 코드
    private BigDecimal baseRate;               // 기본 환율
    private BigDecimal spreadRate;             // 스프레드율 (매매기준율과의 차이)
    private BigDecimal preferentialRate;       // 우대율 (%)
    private BigDecimal fixedFee;               // 고정 수수료
    private BigDecimal feeRate;                // 수수료율 (%)
    private BigDecimal minAmount;              // 최소 환전 금액
    private BigDecimal maxAmount;              // 최대 환전 금액
    private boolean isOnline;                  // 온라인 환전 가능 여부
    private String description;                // 추가 설명
}