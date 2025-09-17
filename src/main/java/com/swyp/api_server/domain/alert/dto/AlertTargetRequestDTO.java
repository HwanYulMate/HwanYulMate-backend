package com.swyp.api_server.domain.alert.dto;

import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Schema(name = "AlertTargetRequestDTO", description = "목표 환율 알림 설정 요청 DTO")
public class AlertTargetRequestDTO {

    @Schema(description = "목표 환율 알림 활성화 여부", example = "true", required = true)
    private boolean enabled;

    @Schema(description = "목표 환율", example = "1350.7", required = false)
    private Double targetPrice;

    @Schema(description = "목표 달성 조건", example = "ABOVE", allowableValues = {"ABOVE", "BELOW"}, required = false)
    private String condition;
}