package com.swyp.api_server.domain.alert.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "목표 환율 알림 설정 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertTargetResponseDTO {
    
    @JsonProperty("currency_code")
    @Schema(description = "통화 코드", example = "USD")
    private String currencyCode;
    
    @JsonProperty("currency_name")
    @Schema(description = "통화명", example = "미국 달러")
    private String currencyName;
    
    @JsonProperty("flag_image_url")
    @Schema(description = "국기 이미지 URL", example = "/images/flags/us.png")
    private String flagImageUrl;
    
    @JsonProperty("is_enabled")
    @Schema(description = "목표 환율 알림 활성화 여부", example = "true")
    private Boolean isEnabled;
    
    @JsonProperty("target_price")
    @Schema(description = "목표 환율", example = "1350.70")
    private BigDecimal targetPrice;
    
    @JsonProperty("condition")
    @Schema(description = "목표 달성 조건", example = "ABOVE", allowableValues = {"ABOVE", "BELOW"})
    private String condition;
}