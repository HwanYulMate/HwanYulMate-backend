package com.swyp.api_server.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OAuth 소셜 로그인 요청 DTO
 * - Apple 로그인의 경우 최초 로그인 시에만 name, email 제공
 * - 재로그인 시에는 accessToken만 제공되므로 name, email은 Optional
 */
@Schema(description = "OAuth 소셜 로그인 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthLoginRequestDto {
    
    @Schema(description = "OAuth 제공자에서 발급받은 액세스 토큰", 
            example = "ya29.a0AfH6SMC...", 
            required = true)
    private String accessToken;
    
    @Schema(description = "사용자 이름 (Apple 로그인 최초 시에만 제공)", 
            example = "홍길동", 
            required = false)
    private String name;
    
    @Schema(description = "사용자 이메일 (Apple 로그인 최초 시에만 제공)", 
            example = "user@icloud.com", 
            required = false)
    private String email;
}