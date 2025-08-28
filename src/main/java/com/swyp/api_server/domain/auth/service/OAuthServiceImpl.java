package com.swyp.api_server.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.api_server.config.security.JwtTokenProvider;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;
import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.entity.User;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OAuth 소셜 로그인 서비스 구현체
 * - Google, Apple OAuth 제공자와 통신하여 사용자 정보 조회
 * - 소셜 로그인 사용자 자동 회원가입 및 JWT 토큰 발급
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final UserRepository userRepository;        // 사용자 데이터 저장소
    private final JwtTokenProvider jwtTokenProvider;    // JWT 토큰 생성기
    private final OkHttpClient httpClient = new OkHttpClient();     // HTTP 클라이언트
    private final ObjectMapper objectMapper = new ObjectMapper();   // JSON 파서

    /**
     * OAuth Authorization Code Flow 처리 (TODO: 향후 구현 예정)
     * @param provider OAuth 제공자
     * @param code OAuth 인증 코드
     * @return JWT 토큰
     * @throws UnsupportedOperationException 아직 구현되지 않음
     */
    @Override
    public TokenResponseDto processOAuthLogin(String provider, String code) {
        // OAuth 인증 코드 플로우는 복잡하므로 일단 기본 구조만 제공
        throw new UnsupportedOperationException("OAuth 코드 플로우는 아직 구현되지 않았습니다.");
    }

    /**
     * 소셜 로그인 처리 (액세스 토큰 기반)
     * @param provider OAuth 제공자 (google, apple)
     * @param accessToken OAuth 제공자에서 발급받은 액세스 토큰
     * @return JWT 토큰 (accessToken, refreshToken)
     * @throws RuntimeException 소셜 로그인 처리 실패 시
     */
    @Override
    public TokenResponseDto processSocialLogin(String provider, String accessToken) {
        try {
            // 1. OAuth 제공자 API를 통해 사용자 정보 조회
            UserInfo userInfo = getUserInfo(provider, accessToken);
            // 2. DB에서 사용자 조회 또는 신규 사용자 등록
            User user = findOrCreateUser(userInfo, provider);
            
            // 3. JWT 토큰 생성
            String jwtAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole());
            String jwtRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
            
            return new TokenResponseDto(jwtAccessToken, jwtRefreshToken);
        } catch (CustomException e) {
            throw e;  // CustomException은 그대로 전파
        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED, e.getMessage(), e);
        }
    }

    /**
     * OAuth 제공자 API로부터 사용자 정보 조회
     * @param provider OAuth 제공자 (google, apple)
     * @param accessToken OAuth 액세스 토큰
     * @return 사용자 기본 정보 (이메일, 이름)
     * @throws IOException API 호출 실패 시
     */
    private UserInfo getUserInfo(String provider, String accessToken) throws IOException {
        // OAuth 제공자별 사용자 정보 API URL 결정
        String url = switch (provider.toLowerCase()) {
            case "google" -> "https://www.googleapis.com/oauth2/v2/userinfo";
            case "apple" -> "https://appleid.apple.com/auth/userinfo";
            default -> throw new CustomException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED, "제공자: " + provider);
        };

        // OAuth 제공자 API에 사용자 정보 요청
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)  // Bearer 토큰 인증
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new CustomException(ErrorCode.OAUTH_USER_INFO_FAILED, 
                    "HTTP Status: " + response.code() + ", Provider: " + provider);
            }

            // API 응답에서 사용자 정보 추출
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 제공자별 JSON 구조에 맞옶 사용자 정보 파싱
            return switch (provider.toLowerCase()) {
                case "google" -> UserInfo.builder()
                        .email(jsonNode.get("email").asText())     // 구글 이메일
                        .name(jsonNode.get("name").asText())       // 구글 닉네임
                        .build();
                case "apple" -> UserInfo.builder()
                        .email(jsonNode.get("email").asText())     // 애플 ID 이메일
                        .name(jsonNode.has("name") ? jsonNode.get("name").asText() : "Apple User")  // 애플은 이름 제공 안할 수 있음
                        .build();
                default -> throw new CustomException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED, "제공자: " + provider);
            };
        }
    }

    /**
     * 소셜 로그인 사용자 조회 또는 신규 등록
     * @param userInfo OAuth 제공자에서 받은 사용자 정보
     * @param provider OAuth 제공자명
     * @return 데이터베이스에 저장된 User 엔티티
     */
    private User findOrCreateUser(UserInfo userInfo, String provider) {
        // 기존 사용자 조회 (이메일 기반)
        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());
        
        if (existingUser.isPresent()) {
            return existingUser.get();  // 기존 사용자 반환
        }

        // 신규 사용자 등록
        User newUser = User.builder()
                .email(userInfo.getEmail())           // 소셜 로그인 이메일
                .userName(userInfo.getName())         // 소셜 로그인 닉네임
                .provider(provider)                   // OAuth 제공자 (google, apple)
                .role("ROLE_USER")                    // 기본 사용자 권한
                .password("")                         // 소셜 로그인 사용자는 비밀번호 없음
                .createdAt(LocalDateTime.now())       // 계정 생성 시간
                .build();

        return userRepository.save(newUser);          // 데이터베이스에 저장
    }

    /**
     * OAuth 제공자에서 받은 사용자 기본 정보를 담는 내부 클래스
     */
    @lombok.Builder
    private static class UserInfo {
        private String email;   // 사용자 이메일
        private String name;    // 사용자 이름/닉네임

        public String getEmail() { return email; }
        public String getName() { return name; }
    }
}