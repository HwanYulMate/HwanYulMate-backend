package com.swyp.api_server.domain.user.service;

import com.swyp.api_server.domain.user.dto.LoginRequestDto;
import com.swyp.api_server.domain.user.dto.SignRequestDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;

public interface UserService {
    boolean signUp(SignRequestDto signRequestDto);
    TokenResponseDto login(LoginRequestDto loginRequestDto);
    TokenResponseDto refreshToken(String refreshToken);
    void logout(String accessToken);
    void withdraw(String email);
    void updateFCMToken(String email, String fcmToken);
    void updateUserName(String email, String newUserName);
}
