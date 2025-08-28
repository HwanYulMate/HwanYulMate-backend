package com.swyp.api_server.domain.alert.dto;

import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Schema(name = "AlertSettingDetailRequestDTO", description = "환율 알림 상세 설정 요청 DTO")
public class AlertSettingDetailRequestDTO {

    @Schema(description = "목표 환율 도달 알림 사용 여부", example = "true")
    private boolean targetPricePush;

    @Schema(description = "오늘의 환율 알림 사용 여부", example = "false")
    private boolean todayExchangeRatePush;

    @Schema(description = "목표 환율", example = "1350.7")
    private double targetPrice;

    @Schema(description = "알림 수단", example = "KAKAO", allowableValues = {"PUSH", "KAKAO"})
    private String targetPricePushHow;

    @Schema(description = "오늘의 환율 알림 발송 시각 (HH:mm, 24h)", example = "09:00", format = "time")
    private String todayExchangeRatePushTime;
}
