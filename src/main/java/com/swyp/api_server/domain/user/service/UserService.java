package com.swyp.api_server.domain.user.service;

import com.swyp.api_server.domain.user.dto.LoginRequestDto;
import com.swyp.api_server.domain.user.dto.SignRequestDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;

public interface UserService {
    boolean signUp(SignRequestDto signRequestDto);
    TokenResponseDto login(LoginRequestDto loginRequestDto);
}
