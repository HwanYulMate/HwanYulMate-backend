package com.swyp.api_server.domain.alert.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 설정 결과 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertSettingResultDTO {
    
    @JsonProperty("success")
    @Schema(description = "성공 여부", example = "true")
    private Boolean success;
    
    @JsonProperty("message")
    @Schema(description = "결과 메시지", example = "목표 환율 알림이 활성화되었습니다.")
    private String message;
    
    @JsonProperty("currency_code")
    @Schema(description = "통화 코드", example = "USD")
    private String currencyCode;
    
    @JsonProperty("alert_type")
    @Schema(description = "알림 타입", example = "TARGET", allowableValues = {"TARGET", "DAILY"})
    private String alertType;
    
    @JsonProperty("status")
    @Schema(description = "알림 상태", example = "ENABLED", allowableValues = {"ENABLED", "DISABLED"})
    private String status;
}