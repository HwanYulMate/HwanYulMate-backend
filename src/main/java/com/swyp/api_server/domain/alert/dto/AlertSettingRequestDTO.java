package com.swyp.api_server.domain.alert.dto;

import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Schema(name = "AlertSettingRequestDTO", description = "알림 설정 항목을 표현하는 DTO")
public class AlertSettingRequestDTO {
    @Schema(description = "통화 티커", example = "USD")
    private String name;
    @Schema(description = "활성화 여부", example = "true")
    private boolean enabled;
}
