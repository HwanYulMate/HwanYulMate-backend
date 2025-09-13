package com.swyp.api_server.domain.alert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 전체 알림 설정 목록 응답 DTO
 * - 사용자의 모든 통화별 알림 설정 조회 시 사용
 */
@Schema(description = "전체 알림 설정 목록 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertSettingListResponseDTO {
    
    @JsonProperty("user_id")
    @Schema(description = "사용자 ID", example = "123")
    private Long userId;
    
    @JsonProperty("alert_settings")
    @Schema(description = "알림 설정 목록")
    private List<AlertSettingResponseDTO> alertSettings;
    
    @JsonProperty("total_count")
    @Schema(description = "전체 알림 설정 개수", example = "3")
    private Integer totalCount;
    
    @JsonProperty("active_count")
    @Schema(description = "활성화된 알림 설정 개수", example = "2")
    private Integer activeCount;
}