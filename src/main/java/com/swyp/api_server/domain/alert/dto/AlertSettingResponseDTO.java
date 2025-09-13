package com.swyp.api_server.domain.alert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 알림 설정 응답 DTO
 * - 사용자의 통화별 알림 설정 정보 조회 시 사용
 */
@Schema(description = "알림 설정 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertSettingResponseDTO {
    
    @JsonProperty("currency_code")
    @Schema(description = "통화 코드", example = "USD")
    private String currencyCode;
    
    @JsonProperty("is_target_price_enabled")
    @Schema(description = "목표 환율 알림 사용 여부", example = "true")
    private Boolean isTargetPriceEnabled;
    
    @JsonProperty("is_daily_alert_enabled")
    @Schema(description = "오늘의 환율 알림 사용 여부", example = "false")
    private Boolean isDailyAlertEnabled;
    
    @JsonProperty("target_price")
    @Schema(description = "목표 환율", example = "1400.00")
    private BigDecimal targetPrice;
    
    @JsonProperty("target_price_push_how")
    @Schema(description = "목표 달성 조건", example = "ABOVE")
    private String targetPricePushHow;
    
    @JsonProperty("daily_alert_time")
    @Schema(description = "일일 알림 시간", example = "09:00:00")
    private String dailyAlertTime;
}