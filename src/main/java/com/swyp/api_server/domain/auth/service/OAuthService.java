package com.swyp.api_server.domain.auth.service;

import com.swyp.api_server.domain.auth.dto.OAuthLoginRequestDto;
import com.swyp.api_server.domain.auth.dto.OAuthLoginResponseDto;

public interface OAuthService {
    OAuthLoginResponseDto processSocialLogin(String provider, OAuthLoginRequestDto requestDto);
}