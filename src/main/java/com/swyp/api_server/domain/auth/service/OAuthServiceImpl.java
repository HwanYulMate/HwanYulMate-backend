package com.swyp.api_server.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.api_server.common.http.CommonHttpClient;
import com.swyp.api_server.common.validator.CommonValidator;
import com.swyp.api_server.config.security.JwtTokenProvider;
import com.swyp.api_server.domain.auth.dto.OAuthLoginRequestDto;
import com.swyp.api_server.domain.auth.dto.OAuthLoginResponseDto;
import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.entity.User;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
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
    private final CommonHttpClient httpClient;          // HTTP 클라이언트
    private final CommonValidator validator;            // 공통 검증기

    /**
     * 소셜 로그인 처리 (Apple 재로그인 지원)
     * @param provider OAuth 제공자 (google, apple)
     * @param requestDto OAuth 로그인 요청 데이터 (accessToken, name, email)
     * @return OAuth 로그인 응답 (JWT 토큰 + 사용자 정보)
     */
    @Override
    public OAuthLoginResponseDto processSocialLogin(String provider, OAuthLoginRequestDto requestDto) {
        try {
            // 1. OAuth 제공자 API를 통해 사용자 정보 조회
            UserInfo userInfo = getUserInfo(provider, requestDto);
            
            // 2. DB에서 사용자 조회 또는 신규 사용자 등록
            UserResult userResult = findOrCreateUser(userInfo, provider);
            User user = userResult.getUser();
            
            // 3. JWT 토큰 생성
            String jwtAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole());
            String jwtRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
            
            // 4. 응답 생성
            return OAuthLoginResponseDto.builder()
                    .accessToken(jwtAccessToken)
                    .refreshToken(jwtRefreshToken)
                    .tokenType("Bearer")
                    .user(OAuthLoginResponseDto.UserInfo.builder()
                            .id(user.getId())
                            .name(user.getUserName())
                            .email(user.getEmail())
                            .provider(user.getProvider())
                            .providerId(user.getProviderId())
                            .build())
                    .isFirstLogin(userResult.isFirstLogin())
                    .build();
                    
        } catch (CustomException e) {
            throw e;  // CustomException은 그대로 전파
        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED, e.getMessage(), e);
        }
    }

    /**
     * OAuth 제공자 API로부터 사용자 정보 조회 (Apple 재로그인 지원)
     * - Apple 재로그인 시 request에서 받은 name, email 사용 (빈 문자열도 허용)
     */
    private UserInfo getUserInfo(String provider, OAuthLoginRequestDto requestDto) {
        // Apple의 경우 재로그인 시에는 API에서 name, email을 제공하지 않으므로
        // request에서 받은 정보를 우선 사용
        if ("apple".equalsIgnoreCase(provider)) {
            // name과 email이 빈 문자열이 아닌 실제 값이 있으면 최초 로그인으로 처리
            if (requestDto.getName() != null && !requestDto.getName().trim().isEmpty() &&
                requestDto.getEmail() != null && !requestDto.getEmail().trim().isEmpty()) {
                // 최초 로그인 시 - request에서 받은 정보 사용
                return UserInfo.builder()
                        .email(requestDto.getEmail())
                        .name(requestDto.getName())
                        .providerId(extractProviderIdFromToken(requestDto.getAccessToken()))
                        .build();
            }
            // 재로그인 시 (name, email이 없거나 빈 문자열) - providerId만 추출하고 나머지는 DB에서 조회
            return UserInfo.builder()
                    .providerId(extractProviderIdFromToken(requestDto.getAccessToken()))
                    .build();
        }
        
        // Google의 경우 기존 방식 사용
        return getUserInfoFromApi(provider, requestDto.getAccessToken());
    }
    
    /**
     * OAuth 제공자 API에서 사용자 정보 조회 (기존 방식)
     */
    private UserInfo getUserInfoFromApi(String provider, String accessToken) {
        String url = switch (provider.toLowerCase()) {
            case "google" -> "https://www.googleapis.com/oauth2/v2/userinfo";
            case "apple" -> "https://appleid.apple.com/auth/userinfo";
            default -> throw new CustomException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED, "제공자: " + provider);
        };

        Map<String, String> headers = Map.of(
            "Authorization", "Bearer " + accessToken
        );

        try {
            JsonNode jsonNode = httpClient.getJson(url, headers);

            return switch (provider.toLowerCase()) {
                case "google" -> UserInfo.builder()
                        .email(jsonNode.get("email").asText())
                        .name(jsonNode.get("name").asText())
                        .providerId(jsonNode.get("id").asText())
                        .build();
                case "apple" -> UserInfo.builder()
                        .email(jsonNode.get("email").asText())
                        .name(jsonNode.has("name") ? jsonNode.get("name").asText() : "Apple User")
                        .providerId(jsonNode.get("sub").asText())
                        .build();
                default -> throw new CustomException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED, "제공자: " + provider);
            };
        } catch (Exception e) {
            log.error("OAuth 사용자 정보 조회 실패: provider={}, error={}", provider, e.getMessage(), e);
            throw new CustomException(ErrorCode.OAUTH_USER_INFO_FAILED, 
                "사용자 정보 조회 실패: " + provider + " - " + e.getMessage());
        }
    }

    /**
     * Apple ID Token에서 providerId (sub) 추출
     * JWT 토큰을 파싱하여 sub 필드 추출 (서명 검증 없이)
     */
    private String extractProviderIdFromToken(String accessToken) {
        try {
            log.debug("Apple ID Token 파싱 시작: {}", accessToken.substring(0, Math.min(50, accessToken.length())) + "...");
            
            // JWT 토큰을 '.'으로 분리 (header.payload.signature)
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format - expected 3 parts but got " + parts.length);
            }
            
            // Payload 부분(두 번째 부분)을 Base64 디코딩
            String payload = parts[1];
            
            // Base64 URL-safe 디코딩
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);
            
            log.debug("Decoded payload: {}", decodedPayload);
            
            // JSON 파싱하여 sub 필드 추출
            ObjectMapper mapper = new ObjectMapper();
            JsonNode claims = mapper.readTree(decodedPayload);
            
            // sub 필드 추출 (Apple 사용자 고유 ID)
            if (!claims.has("sub")) {
                throw new IllegalArgumentException("sub field not found in JWT payload");
            }
            
            String sub = claims.get("sub").asText();
            if (sub == null || sub.isEmpty()) {
                throw new IllegalArgumentException("sub field is empty");
            }
            
            log.info("Apple ID Token 파싱 성공 - providerId: {}", sub);
            return sub;
            
        } catch (IllegalArgumentException e) {
            log.error("Apple ID Token 형식 오류: {}", e.getMessage());
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID, "Apple ID Token 형식이 올바르지 않습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("Apple ID Token 파싱 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID, "Apple ID Token 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * 소셜 로그인 사용자 조회 또는 신규 등록 (Apple 재로그인 지원)
     */
    private UserResult findOrCreateUser(UserInfo userInfo, String provider) {
        // Apple 재로그인의 경우 providerId로만 조회 (email이 null이거나 빈 문자열인 경우)
        if ("apple".equalsIgnoreCase(provider) && 
            (userInfo.getEmail() == null || userInfo.getEmail().trim().isEmpty())) {
            Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider.toUpperCase(), userInfo.getProviderId());
            if (existingUser.isPresent()) {
                return UserResult.builder()
                        .user(existingUser.get())
                        .isFirstLogin(false)
                        .build();
            }
            throw new CustomException(ErrorCode.USER_NOT_FOUND, "Apple 사용자를 찾을 수 없습니다.");
        }
        
        // 이메일 검증
        validator.validateEmailFormat(userInfo.getEmail());
        
        // 기존 사용자 조회 (이메일 + provider + providerId)
        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // providerId가 없으면 업데이트
            if (user.getProviderId() == null && userInfo.getProviderId() != null) {
                user.setProviderId(userInfo.getProviderId());
                userRepository.save(user);
            }
            return UserResult.builder()
                    .user(user)
                    .isFirstLogin(false)
                    .build();
        }

        // 신규 사용자 등록
        User newUser = User.builder()
                .email(userInfo.getEmail())
                .userName(userInfo.getName())
                .provider(provider.toUpperCase())
                .providerId(userInfo.getProviderId())
                .role("ROLE_USER")
                .password("")
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);
        return UserResult.builder()
                .user(savedUser)
                .isFirstLogin(true)
                .build();
    }

    /**
     * OAuth 제공자에서 받은 사용자 기본 정보를 담는 내부 클래스
     */
    @lombok.Builder
    private static class UserInfo {
        private String email;      // 사용자 이메일
        private String name;       // 사용자 이름/닉네임
        private String providerId; // OAuth 제공자 사용자 ID

        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getProviderId() { return providerId; }
    }
    
    /**
     * 사용자 조회/생성 결과를 담는 내부 클래스
     */
    @lombok.Builder
    private static class UserResult {
        private User user;
        private boolean isFirstLogin;
        
        public User getUser() { return user; }
        public boolean isFirstLogin() { return isFirstLogin; }
    }
}