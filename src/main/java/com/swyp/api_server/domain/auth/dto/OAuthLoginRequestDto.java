package com.swyp.api_server.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OAuth 소셜 로그인 요청 DTO (통일된 구조)
 * - Apple, Google 모두 3개 필드를 모두 포함하여 전송
 * - Apple 최초 로그인: 실제 name, email 값 전송
 * - Apple 재로그인: name, email을 빈 문자열로 전송
 * - Google: 항상 3개 필드 모두 포함
 */
@Schema(description = "OAuth 소셜 로그인 요청 (통일된 구조)")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthLoginRequestDto {
    
    @Schema(description = "OAuth 제공자에서 발급받은 액세스 토큰", 
            example = "ya29.a0AfH6SMC...", 
            required = true)
    private String accessToken;
    
    @Schema(description = "사용자 이름 (Apple 재로그인 시 빈 문자열, Google은 항상 포함)", 
            example = "홍길동", 
            required = true)
    private String name;
    
    @Schema(description = "사용자 이메일 (Apple 재로그인 시 빈 문자열, Google은 항상 포함)", 
            example = "user@icloud.com", 
            required = true)
    private String email;
}