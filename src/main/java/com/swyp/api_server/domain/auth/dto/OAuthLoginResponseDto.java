package com.swyp.api_server.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OAuth 소셜 로그인 응답 DTO
 * - JWT 토큰과 사용자 정보를 함께 반환
 * - Apple 재로그인 시에도 서버에 저장된 사용자 정보 제공
 */
@Schema(description = "OAuth 소셜 로그인 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthLoginResponseDto {
    
    @Schema(description = "JWT 액세스 토큰", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
    private String accessToken;
    
    @Schema(description = "JWT 리프레시 토큰", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
    
    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;
    
    @Schema(description = "사용자 정보")
    private UserInfo user;
    
    @Schema(description = "최초 로그인 여부", example = "true")
    private boolean isFirstLogin;
    
    @Schema(description = "사용자 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        
        @Schema(description = "사용자 ID", example = "123")
        private Long id;
        
        @Schema(description = "사용자 이름", example = "홍길동")
        private String name;
        
        @Schema(description = "사용자 이메일", example = "user@icloud.com")
        private String email;
        
        @Schema(description = "OAuth 제공자", example = "APPLE")
        private String provider;
        
        @Schema(description = "OAuth 제공자 사용자 ID", example = "001234.567890abcdef...")
        private String providerId;
    }
}