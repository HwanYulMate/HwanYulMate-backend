package com.swyp.api_server.domain.auth.service;

import com.swyp.api_server.domain.auth.dto.OAuthLoginRequestDto;
import com.swyp.api_server.domain.auth.dto.OAuthLoginResponseDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;

public interface OAuthService {
    TokenResponseDto processOAuthLogin(String provider, String code);
    OAuthLoginResponseDto processSocialLogin(String provider, OAuthLoginRequestDto requestDto);
}