package com.swyp.api_server.domain.auth.controller;

import com.swyp.api_server.domain.auth.dto.OAuthLoginRequestDto;
import com.swyp.api_server.domain.auth.dto.OAuthLoginResponseDto;
import com.swyp.api_server.domain.auth.service.OAuthService;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.swyp.api_server.common.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth 소셜 로그인 컨트롤러
 * - Google, Apple OAuth 로그인 처리
 * - 외부 OAuth 제공자에서 받은 인증 코드 또는 액세스 토큰으로 JWT 토큰 발급
 */
@Tag(name = "OAuth", description = "소셜 로그인 API (Google, Apple)")
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    /**
     * OAuth 콜백 처리 (Authorization Code Flow)
     * - OAuth 제공자에서 리다이렉트되는 콜백 URL
     * @param provider OAuth 제공자 (google, apple)
     * @param code OAuth 인증 코드
     * @return JWT 토큰 또는 오류 메시지
     */
    @Operation(summary = "OAuth 콜백 처리", description = "OAuth Authorization Code Flow를 통한 소셜 로그인 콜백 처리 (현재 미구현)")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = TokenResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "OAuth 로그인 실패 (잘못된 코드, 제공자 오류 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "501", 
            description = "미구현 기능",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/callback/{provider}")
    public ResponseEntity<?> oauthCallback(
        @Parameter(description = "OAuth 제공자", example = "google") @PathVariable String provider,
        @Parameter(description = "OAuth 인증 코드") @RequestParam String code) {
        // CustomException으로 예외 처리됨 - GlobalExceptionHandler에서 일괄 처리
        TokenResponseDto tokenResponse = oAuthService.processOAuthLogin(provider, code);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 소셜 로그인 처리 (Access Token 기반)
     * - 모바일 앱에서 OAuth 제공자로부터 직접 받은 액세스 토큰을 사용
     * @param provider OAuth 제공자 (google, apple)
     * @param accessToken OAuth 제공자에서 발급받은 액세스 토큰
     * @return JWT 토큰 또는 오류 메시지
     */
    @Operation(summary = "소셜 로그인", 
        description = "OAuth 액세스 토큰을 사용하여 소셜 로그인을 처리합니다. 모바일 앱에서 주로 사용됩니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "소셜 로그인 성공",
            content = @Content(schema = @Schema(implementation = TokenResponseDto.class),
                examples = @ExampleObject(name = "성공 응답", 
                    value = "{\"accessToken\": \"eyJ0eXAiOiJKV1QiLi4.\", \"refreshToken\": \"eyJ0eXAiOiJKV1QiLi4.\", \"tokenType\": \"Bearer\"}"))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "소셜 로그인 실패 (잘못된 액세스 토큰, 지원하지 않는 제공자 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패 (만료된 또는 잘못된 액세스 토큰)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/{provider}")
    public ResponseEntity<?> socialLogin(
        @Parameter(description = "OAuth 제공자", example = "google") @PathVariable String provider,
        @Parameter(description = "OAuth 제공자에서 발급받은 액세스 토큰", example = "ya29.a0AfH6SMC...") @RequestParam String accessToken) {
        // CustomException으로 예외 처리됨 - GlobalExceptionHandler에서 일괄 처리
        TokenResponseDto tokenResponse = oAuthService.processSocialLogin(provider, accessToken);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 소셜 로그인 V2 - Apple 재로그인 지원 개선
     * - Apple 최초 로그인 시 name, email 정보 저장
     * - Apple 재로그인 시 서버에서 저장된 사용자 정보 반환
     * @param provider OAuth 제공자 (google, apple)
     * @param requestDto OAuth 로그인 요청 데이터
     * @return OAuth 로그인 응답 (JWT 토큰 + 사용자 정보)
     */
    @Operation(summary = "소셜 로그인 V2 (Apple 재로그인 지원)", 
        description = "Apple 재로그인 문제를 해결한 개선된 소셜 로그인 API입니다. " +
                     "Apple 최초 로그인 시에는 name, email을 함께 전송하고, " +
                     "재로그인 시에는 accessToken만 전송하면 서버에서 저장된 사용자 정보를 반환합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "소셜 로그인 성공",
            content = @Content(schema = @Schema(implementation = OAuthLoginResponseDto.class),
                examples = @ExampleObject(name = "성공 응답", 
                    value = "{\n" +
                            "  \"accessToken\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...\",\n" +
                            "  \"refreshToken\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...\",\n" +
                            "  \"tokenType\": \"Bearer\",\n" +
                            "  \"user\": {\n" +
                            "    \"id\": 123,\n" +
                            "    \"name\": \"홍길동\",\n" +
                            "    \"email\": \"user@icloud.com\",\n" +
                            "    \"provider\": \"APPLE\",\n" +
                            "    \"providerId\": \"001234.567890abcdef...\"\n" +
                            "  },\n" +
                            "  \"isFirstLogin\": false\n" +
                            "}"))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "소셜 로그인 실패 (잘못된 액세스 토큰, 지원하지 않는 제공자 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패 (만료된 또는 잘못된 액세스 토큰)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Apple 재로그인 시 사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/v2/{provider}")
    public ResponseEntity<OAuthLoginResponseDto> socialLoginV2(
        @Parameter(description = "OAuth 제공자", example = "apple") @PathVariable String provider,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "OAuth 로그인 요청 데이터",
            content = @Content(
                examples = {
                    @ExampleObject(
                        name = "Apple 최초 로그인",
                        description = "Apple 최초 로그인 시 name, email 포함",
                        value = "{\n" +
                                "  \"accessToken\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...\",\n" +
                                "  \"name\": \"홍길동\",\n" +
                                "  \"email\": \"user@icloud.com\"\n" +
                                "}"
                    ),
                    @ExampleObject(
                        name = "Apple 재로그인",
                        description = "Apple 재로그인 시 accessToken만 전송",
                        value = "{\n" +
                                "  \"accessToken\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...\"\n" +
                                "}"
                    ),
                    @ExampleObject(
                        name = "Google 로그인",
                        description = "Google은 항상 API에서 사용자 정보 조회",
                        value = "{\n" +
                                "  \"accessToken\": \"ya29.a0AfH6SMC...\"\n" +
                                "}"
                    )
                }
            )
        ) @RequestBody OAuthLoginRequestDto requestDto) {
        
        OAuthLoginResponseDto response = oAuthService.processSocialLoginV2(provider, requestDto);
        return ResponseEntity.ok(response);
    }
}