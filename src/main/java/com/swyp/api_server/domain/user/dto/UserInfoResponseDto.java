package com.swyp.api_server.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정보 조회 응답 DTO
 * - 민감한 정보(비밀번호, FCM토큰 등)는 제외하고 필요한 정보만 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponseDto {
    
    private String email;           // 이메일 (읽기 전용)
    private String userName;        // 사용자 이름
    private String provider;        // 로그인 제공자 (google, apple, local)
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt; // 가입일
    
    private boolean isDeleted;       // 탈퇴 여부
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") 
    private LocalDateTime deletedAt; // 탈퇴 일시 (탈퇴한 경우만)
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finalDeletionDate; // 완전 삭제 예정일 (탈퇴한 경우만)
}