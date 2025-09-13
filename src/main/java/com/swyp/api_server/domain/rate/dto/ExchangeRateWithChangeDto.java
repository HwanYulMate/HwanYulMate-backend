package com.swyp.api_server.domain.rate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swyp.api_server.entity.ExchangeRate;
import com.swyp.api_server.domain.rate.entity.ExchangeRateHistory;
import com.swyp.api_server.domain.rate.ExchangeList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 변동률이 포함된 환율 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExchangeRateWithChange", description = "변동률이 포함된 환율 정보")
public class ExchangeRateWithChangeDto {
    
    @JsonProperty("currency_code")
    @Schema(description = "통화 코드", example = "USD")
    private String currencyCode;
    
    @JsonProperty("currency_name")
    @Schema(description = "통화명", example = "미국 달러")
    private String currencyName;
    
    @JsonProperty("flag_image_url")
    @Schema(description = "국기 이미지 URL", example = "/images/flags/us.png")
    private String flagImageUrl;
    
    @JsonProperty("exchange_rate")
    @Schema(description = "현재 환율", example = "1389.40")
    private BigDecimal exchangeRate;
    
    @JsonProperty("base_date")
    @Schema(description = "기준 날짜", example = "20250913")
    private String baseDate;
    
    // 변동 정보
    @JsonProperty("change_amount")
    @Schema(description = "변동 금액 (원)", example = "-0.70")
    private BigDecimal changeAmount;
    
    @JsonProperty("change_percent")
    @Schema(description = "변동 퍼센트 (%)", example = "-0.05")
    private Double changePercent;
    
    @JsonProperty("change_direction")
    @Schema(description = "변동 방향", example = "DOWN", allowableValues = {"UP", "DOWN", "STABLE"})
    private String changeDirection;
    
    /**
     * 현재 환율과 전일 환율로부터 변동률 계산하여 DTO 생성
     */
    public static ExchangeRateWithChangeDto of(ExchangeRate current, ExchangeRateHistory previous) {
        ExchangeRateWithChangeDto dto = new ExchangeRateWithChangeDto();
        
        // 기본 환율 정보
        dto.currencyCode = current.getCurrencyCode();
        dto.currencyName = current.getCurrencyName();
        dto.flagImageUrl = getFlagImageUrl(current.getCurrencyCode());
        dto.exchangeRate = current.getExchangeRate();
        dto.baseDate = current.getBaseDate();
        
        // 변동률 계산
        if (previous != null) {
            // 변동 금액 계산
            dto.changeAmount = current.getExchangeRate().subtract(previous.getExchangeRate())
                    .setScale(2, RoundingMode.HALF_UP);
            
            // 변동 퍼센트 계산 ((현재값 - 이전값) / 이전값 * 100)
            BigDecimal changeRatio = dto.changeAmount.divide(previous.getExchangeRate(), 6, RoundingMode.HALF_UP);
            dto.changePercent = changeRatio.multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            
            // 변동 방향 설정
            int comparison = dto.changeAmount.compareTo(BigDecimal.ZERO);
            if (comparison > 0) {
                dto.changeDirection = "UP";
            } else if (comparison < 0) {
                dto.changeDirection = "DOWN";
            } else {
                dto.changeDirection = "STABLE";
            }
        } else {
            // 이전 데이터가 없는 경우 (첫날)
            dto.changeAmount = BigDecimal.ZERO;
            dto.changePercent = 0.0;
            dto.changeDirection = "STABLE";
        }
        
        return dto;
    }
    
    /**
     * 현재 환율만으로 기본 DTO 생성 (변동률 없음)
     */
    public static ExchangeRateWithChangeDto from(ExchangeRate current) {
        return of(current, null);
    }
    
    /**
     * 통화 코드에 해당하는 국기 이미지 URL 조회
     */
    private static String getFlagImageUrl(String currencyCode) {
        try {
            return ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase()).getFlagImageUrl();
        } catch (IllegalArgumentException e) {
            return "/images/flags/default.png"; // 기본 이미지 반환
        }
    }
}