package com.swyp.api_server.domain.user.service;

import com.swyp.api_server.config.security.JwtTokenProvider;
import com.swyp.api_server.domain.user.dto.LoginRequestDto;
import com.swyp.api_server.domain.user.dto.SignRequestDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;
import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.entity.User;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean signUp(SignRequestDto signRequestDto) {
        if(userRepository.existsByEmail(signRequestDto.getEmail())) {
            throw new CustomException(ErrorCode.USER_ALREADY_EXISTS, "이메일: " + signRequestDto.getEmail());
        }

        User user = User.builder()
                .email(signRequestDto.getEmail())
                .password(passwordEncoder.encode(signRequestDto.getPassword()))
                .provider("local")
                .role("ROLE_USER")
                .userName(signRequestDto.getUserName())
                .createdAt(LocalDateTime.now())
                .build();
        try {
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.USER_REGISTRATION_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public TokenResponseDto login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + loginRequestDto.getEmail()));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        return new TokenResponseDto(accessToken, refreshToken);
    }

    @Override
    public TokenResponseDto refreshToken(String refreshToken) {
        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN, "Refresh Token이 만료되었습니다.");
        }

        // Refresh Token 타입 확인
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_TYPE, "유효하지 않은 토큰 타입입니다.");
        }

        // Refresh Token에서 사용자 이메일 추출
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 사용자 존재 여부 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));

        // 새로운 Access Token과 Refresh Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String accessToken) {
        // Access Token 유효성 검증
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 Access Token입니다.");
        }

        // Access Token 타입 확인
        if (!jwtTokenProvider.isAccessToken(accessToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_TYPE, "Access Token이 아닙니다.");
        }

        // 로그아웃 처리 (JWT는 stateless이므로 클라이언트에서 토큰 삭제)
        // 필요시 Redis에 블랙리스트 토큰 저장 가능
        
        String email = jwtTokenProvider.getEmailFromToken(accessToken);
        // 로그 기록
        // log.info("사용자 로그아웃: {}", email);
    }

    @Override
    public void withdraw(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));

        // 이미 탈퇴한 사용자인지 확인
        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 탈퇴 처리된 사용자입니다.");
        }

        // 30일 보관 탈퇴 처리
        user.withdraw();
        userRepository.save(user);
        
        // 탈퇴 처리 로그
        // log.info("회원 탈퇴 처리: {}, 최종 삭제 예정일: {}", email, user.getFinalDeletionDate());
    }

    @Override
    public void updateFCMToken(String email, String fcmToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));
        
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        
        // log.info("FCM 토큰 업데이트: {}", email);
    }
}
