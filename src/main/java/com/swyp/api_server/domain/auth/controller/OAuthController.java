package com.swyp.api_server.domain.auth.controller;

import com.swyp.api_server.domain.auth.dto.OAuthLoginRequestDto;
import com.swyp.api_server.domain.auth.dto.OAuthLoginResponseDto;
import com.swyp.api_server.domain.auth.service.OAuthService;
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
     * 소셜 로그인 (통일된 API 구조)
     * - Apple, Google 모두 통일된 3개 필드 구조 사용
     * - Apple 최초 로그인 시 name, email 정보 저장
     * - Apple 재로그인 시 name, email을 빈 문자열로 전송
     * - Google은 항상 API에서 사용자 정보 조회
     * @param provider OAuth 제공자 (google, apple)
     * @param requestDto OAuth 로그인 요청 데이터
     * @return OAuth 로그인 응답 (JWT 토큰 + 사용자 정보)
     */
    @Operation(summary = "소셜 로그인 (통일된 API 구조)", 
        description = "Apple과 Google OAuth API 구조를 통일한 소셜 로그인 API입니다. " +
                     "Apple 최초 로그인 시에는 실제 name, email을 전송하고, " +
                     "재로그인 시에는 name, email을 빈 문자열로 전송합니다. " +
                     "Google은 항상 3개 필드를 모두 포함하여 전송합니다.")
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
    @PostMapping("/{provider}")
    public ResponseEntity<OAuthLoginResponseDto> socialLogin(
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
                        description = "Apple 재로그인 시 name, email을 빈 문자열로 전송",
                        value = "{\n" +
                                "  \"accessToken\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...\",\n" +
                                "  \"name\": \"\",\n" +
                                "  \"email\": \"\"\n" +
                                "}"
                    ),
                    @ExampleObject(
                        name = "Google 로그인",
                        description = "Google은 항상 3개 필드 모두 포함",
                        value = "{\n" +
                                "  \"accessToken\": \"ya29.a0AfH6SMC...\",\n" +
                                "  \"name\": \"홍길동\",\n" +
                                "  \"email\": \"user@gmail.com\"\n" +
                                "}"
                    )
                }
            )
        ) @RequestBody OAuthLoginRequestDto requestDto) {
        
        OAuthLoginResponseDto response = oAuthService.processSocialLogin(provider, requestDto);
        return ResponseEntity.ok(response);
    }
}