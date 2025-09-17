package com.swyp.api_server.domain.user.service;

import com.swyp.api_server.domain.user.dto.LoginRequestDto;
import com.swyp.api_server.domain.user.dto.SignRequestDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;
import com.swyp.api_server.domain.user.dto.UserInfoResponseDto;

public interface UserService {
    boolean signUp(SignRequestDto signRequestDto);
    TokenResponseDto login(LoginRequestDto loginRequestDto);
    TokenResponseDto refreshToken(String refreshToken);
    void logout(String accessToken);
    void logoutApple(String accessToken, String appleRefreshToken);
    void withdraw(String email, String reason);
    void withdrawApple(String email, String reason, String appleRefreshToken);
    void updateFCMToken(String email, String fcmToken);
    void updateUserName(String email, String newUserName);
    UserInfoResponseDto getUserInfo(String email);
}
