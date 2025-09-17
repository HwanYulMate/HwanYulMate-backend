package com.swyp.api_server.domain.user.service;

import com.swyp.api_server.config.security.JwtTokenProvider;
import com.swyp.api_server.domain.auth.service.AppleTokenValidator;
import com.swyp.api_server.domain.user.dto.LoginRequestDto;
import com.swyp.api_server.domain.user.dto.SignRequestDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;
import com.swyp.api_server.domain.user.dto.UserInfoResponseDto;
import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.entity.User;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppleTokenValidator appleTokenValidator;

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

        // 사용자 정보 조회
        String email = jwtTokenProvider.getEmailFromToken(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));

        // Apple 사용자인 경우 Apple 토큰 무효화
        if (user.isAppleUser() && user.getAppleRefreshToken() != null) {
            appleTokenValidator.revokeAppleToken(user.getAppleRefreshToken(), user.getProviderId());
            
            // Apple refresh token 삭제
            user.setAppleRefreshToken(null);
            userRepository.save(user);
            
            log.info("Apple 사용자 로그아웃 (자동 감지): {}", email);
        } else {
            log.info("일반 사용자 로그아웃: {}", email);
        }
    }

    @Override
    public void withdraw(String email, String reason) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));

        // 이미 탈퇴한 사용자인지 확인
        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 탈퇴 처리된 사용자입니다.");
        }

        // Apple 사용자인 경우 Apple 토큰 무효화 및 연동 해제
        if (user.isAppleUser()) {
            if (user.getAppleRefreshToken() != null) {
                appleTokenValidator.revokeAppleToken(user.getAppleRefreshToken(), user.getProviderId());
            }
            appleTokenValidator.disconnectAppleAccount(user.getProviderId());
            
            // Apple refresh token 삭제
            user.setAppleRefreshToken(null);
            
            log.info("Apple 사용자 탈퇴 처리 (자동 감지): {}", email);
        } else {
            log.info("일반 사용자 탈퇴 처리: {}", email);
        }

        // 30일 보관 탈퇴 처리 (이유 포함)
        user.withdraw(reason);
        userRepository.save(user);
        
        log.info("회원 탈퇴 처리 완료: {}, 최종 삭제 예정일: {}", email, user.getFinalDeletionDate());
    }

    @Override
    public void logoutApple(String accessToken, String appleRefreshToken) {
        // 기본 로그아웃 처리
        logout(accessToken);
        
        // Apple 토큰 무효화
        String email = jwtTokenProvider.getEmailFromToken(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));
                
        if (user.isAppleUser() && appleRefreshToken != null && !appleRefreshToken.trim().isEmpty()) {
            appleTokenValidator.revokeAppleToken(appleRefreshToken, user.getProviderId());
            
            // Apple refresh token 삭제
            user.setAppleRefreshToken(null);
            userRepository.save(user);
        }
        
        log.info("Apple 사용자 로그아웃 완료: {}", email);
    }

    @Override
    public void withdrawApple(String email, String reason, String appleRefreshToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));

        // 이미 탈퇴한 사용자인지 확인
        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 탈퇴 처리된 사용자입니다.");
        }

        // Apple 사용자인 경우 Apple 토큰 무효화 및 연동 해제
        if (user.isAppleUser()) {
            if (appleRefreshToken != null && !appleRefreshToken.trim().isEmpty()) {
                appleTokenValidator.revokeAppleToken(appleRefreshToken, user.getProviderId());
            }
            appleTokenValidator.disconnectAppleAccount(user.getProviderId());
            
            // Apple refresh token 삭제
            user.setAppleRefreshToken(null);
        }

        // 30일 보관 탈퇴 처리 (이유 포함)
        user.withdraw(reason);
        userRepository.save(user);
        
        log.info("Apple 사용자 탈퇴 처리: {}, 최종 삭제 예정일: {}", email, user.getFinalDeletionDate());
    }

    @Override
    public void updateFCMToken(String email, String fcmToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));
        
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        
        // log.info("FCM 토큰 업데이트: {}", email);
    }

    @Override
    public void updateUserName(String email, String newUserName) {
        // 사용자 존재 여부 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));
        
        // 탈퇴한 사용자인지 확인
        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "탈퇴한 사용자는 이름을 변경할 수 없습니다.");
        }
        
        // 이름 유효성 검증
        validateUserName(newUserName, user.getUserName());
        
        // 이름 변경
        user.setUserName(newUserName);
        userRepository.save(user);
        
        // log.info("사용자 이름 변경: {} -> {}", email, newUserName);
    }
    
    /**
     * 사용자 이름 유효성 검증
     * @param newUserName 새 이름
     * @param currentUserName 현재 이름
     */
    private void validateUserName(String newUserName, String currentUserName) {
        // null 또는 빈 문자열 검증
        if (newUserName == null || newUserName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이름은 비어있을 수 없습니다.");
        }
        
        // 공백 제거 후 재검증
        newUserName = newUserName.trim();
        if (newUserName.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이름은 비어있을 수 없습니다.");
        }
        
        // 길이 검증 (1-10자) - 실제 서비스 수준
        if (newUserName.length() > 10) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이름은 10자 이하로 입력해주세요.");
        }
        
        // 현재 이름과 동일한지 검증
        if (newUserName.equals(currentUserName)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "현재 이름과 동일합니다.");
        }
        
        // 특수문자 제한 (선택적)
        if (!newUserName.matches("^[a-zA-Z가-힣0-9\\s]+$")) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이름에는 한글, 영문, 숫자만 사용할 수 있습니다.");
        }
    }

    @Override
    public UserInfoResponseDto getUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email));
        
        return UserInfoResponseDto.builder()
                .email(user.getEmail())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .isDeleted(user.getIsDeleted() != null ? user.getIsDeleted() : false)
                .deletedAt(user.getDeletedAt())
                .finalDeletionDate(user.getFinalDeletionDate())
                .build();
    }
}
