package com.swyp.api_server.domain.auth.service;

import com.swyp.api_server.domain.user.dto.TokenResponseDto;

public interface OAuthService {
    TokenResponseDto processOAuthLogin(String provider, String code);
    TokenResponseDto processSocialLogin(String provider, String accessToken);
}