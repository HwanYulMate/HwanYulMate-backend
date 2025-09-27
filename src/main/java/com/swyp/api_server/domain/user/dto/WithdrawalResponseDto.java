package com.swyp.api_server.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 회원 탈퇴 응답 DTO
 * 탈퇴 처리 상태와 30일 안내 정보를 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 탈퇴 응답")
public class WithdrawalResponseDto {
    
    @Schema(description = "탈퇴 처리 성공 여부", example = "true")
    private boolean success;
    
    @Schema(description = "응답 메시지", example = "회원 탈퇴가 처리되었습니다.")
    private String message;
    
    @Schema(description = "탈퇴 처리 일시", example = "2024-01-15T14:30:00")
    private LocalDateTime withdrawalDate;
    
    @Schema(description = "최종 삭제 예정일 (30일 후)", example = "2024-02-14T14:30:00")
    private LocalDateTime finalDeletionDate;
    
    @Schema(description = "30일 내 데이터 복구 가능 여부", example = "true")
    private boolean canRecover;
    
    @Schema(description = "로그아웃 권장 여부", example = "true") 
    private boolean shouldLogout;
    
    @Schema(description = "추가 안내 사항")
    private String notice;
    
    /**
     * 성공적인 탈퇴 처리 응답 생성
     */
    public static WithdrawalResponseDto success(LocalDateTime withdrawalDate, LocalDateTime finalDeletionDate) {
        return WithdrawalResponseDto.builder()
            .success(true)
            .message("회원 탈퇴가 처리되었습니다.")
            .withdrawalDate(withdrawalDate)
            .finalDeletionDate(finalDeletionDate)
            .canRecover(true)
            .shouldLogout(true)
            .notice("30일 내 재가입하면 정보 이용이 유지됩니다. 로그아웃을 권장합니다.")
            .build();
    }
}