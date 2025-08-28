package com.swyp.api_server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "로그인 요청 DTO")
@Getter
@NoArgsConstructor
public class LoginRequestDto {
    @Schema(description = "이메일 주소", example = "user@example.com", required = true)
    private String email;
    
    @Schema(description = "비밀번호", example = "password123", required = true)
    private String password;
}