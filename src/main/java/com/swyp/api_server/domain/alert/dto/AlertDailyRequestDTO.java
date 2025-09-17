package com.swyp.api_server.domain.alert.dto;

import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Schema(name = "AlertDailyRequestDTO", description = "일일 환율 알림 설정 요청 DTO")
public class AlertDailyRequestDTO {

    @Schema(description = "알림 발송 시간 (HH:mm, 24시간 형식)", example = "09:00", pattern = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", required = true)
    private String alertTime;
}