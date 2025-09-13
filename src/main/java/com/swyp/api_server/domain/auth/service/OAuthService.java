package com.swyp.api_server.domain.auth.service;

import com.swyp.api_server.domain.auth.dto.OAuthLoginRequestDto;
import com.swyp.api_server.domain.auth.dto.OAuthLoginResponseDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;

public interface OAuthService {
    TokenResponseDto processOAuthLogin(String provider, String code);
    TokenResponseDto processSocialLogin(String provider, String accessToken);
    OAuthLoginResponseDto processSocialLoginV2(String provider, OAuthLoginRequestDto requestDto);
}