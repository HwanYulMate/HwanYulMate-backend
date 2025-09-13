package com.swyp.api_server.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.swyp.api_server.common.http.CommonHttpClient;
import com.swyp.api_server.common.validator.CommonValidator;
import com.swyp.api_server.config.security.JwtTokenProvider;
import com.swyp.api_server.domain.auth.dto.OAuthLoginRequestDto;
import com.swyp.api_server.domain.auth.dto.OAuthLoginResponseDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;
import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.entity.User;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private UserInfo getUserInfo(String provider, String accessToken) {
        // OAuth 제공자별 사용자 정보 API URL 결정
        String url = switch (provider.toLowerCase()) {
            case "google" -> "https://www.googleapis.com/oauth2/v2/userinfo";
            case "apple" -> "https://appleid.apple.com/auth/userinfo";
            default -> throw new CustomException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED, "제공자: " + provider);
        };

        // OAuth 제공자 API에 사용자 정보 요청
        Map<String, String> headers = Map.of(
            "Authorization", "Bearer " + accessToken
        );

        try {
            JsonNode jsonNode = httpClient.getJson(url, headers);

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
        } catch (Exception e) {
            log.error("OAuth 사용자 정보 조회 실패: provider={}, error={}", provider, e.getMessage(), e);
            throw new CustomException(ErrorCode.OAUTH_USER_INFO_FAILED, 
                "사용자 정보 조회 실패: " + provider + " - " + e.getMessage());
        }
    }

    /**
     * 소셜 로그인 사용자 조회 또는 신규 등록
     * @param userInfo OAuth 제공자에서 받은 사용자 정보
     * @param provider OAuth 제공자명
     * @return 데이터베이스에 저장된 User 엔티티
     */
    private User findOrCreateUser(UserInfo userInfo, String provider) {
        // 이메일 형식 검증
        validator.validateEmailFormat(userInfo.getEmail());
        
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
     * 소셜 로그인 처리 V2 (개선된 버전 - Apple 재로그인 지원)
     * @param provider OAuth 제공자 (google, apple)
     * @param requestDto OAuth 로그인 요청 데이터 (accessToken, name, email)
     * @return OAuth 로그인 응답 (JWT 토큰 + 사용자 정보)
     */
    @Override
    public OAuthLoginResponseDto processSocialLoginV2(String provider, OAuthLoginRequestDto requestDto) {
        try {
            // 1. OAuth 제공자 API를 통해 사용자 정보 조회
            UserInfo userInfo = getUserInfoV2(provider, requestDto);
            
            // 2. DB에서 사용자 조회 또는 신규 사용자 등록
            UserResult userResult = findOrCreateUserV2(userInfo, provider);
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
            log.error("소셜 로그인 V2 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED, e.getMessage(), e);
        }
    }

    /**
     * OAuth 제공자 API로부터 사용자 정보 조회 V2 (개선된 버전)
     * - Apple 재로그인 시 request에서 받은 name, email 사용
     */
    private UserInfo getUserInfoV2(String provider, OAuthLoginRequestDto requestDto) {
        // Apple의 경우 재로그인 시에는 API에서 name, email을 제공하지 않으므로
        // request에서 받은 정보를 우선 사용
        if ("apple".equalsIgnoreCase(provider)) {
            if (requestDto.getName() != null && requestDto.getEmail() != null) {
                // 최초 로그인 시 - request에서 받은 정보 사용
                return UserInfo.builder()
                        .email(requestDto.getEmail())
                        .name(requestDto.getName())
                        .providerId(extractProviderIdFromToken(requestDto.getAccessToken()))
                        .build();
            }
            // 재로그인 시 - providerId만 추출하고 나머지는 DB에서 조회
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
     */
    private String extractProviderIdFromToken(String accessToken) {
        // 실제로는 JWT 파싱이 필요하지만, 여기서는 간단하게 처리
        // 실제 구현 시에는 JWT 라이브러리를 사용해야 함
        try {
            // TODO: JWT 토큰 파싱하여 sub 필드 추출
            return "temp_provider_id_" + System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Apple ID Token 파싱 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID, "Apple ID Token 파싱 실패");
        }
    }

    /**
     * 소셜 로그인 사용자 조회 또는 신규 등록 V2
     */
    private UserResult findOrCreateUserV2(UserInfo userInfo, String provider) {
        // Apple 재로그인의 경우 providerId로만 조회
        if ("apple".equalsIgnoreCase(provider) && userInfo.getEmail() == null) {
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