package com.swyp.api_server.domain.user.controller;

import com.swyp.api_server.domain.user.dto.LoginRequestDto;
import com.swyp.api_server.domain.user.dto.SignRequestDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;
import com.swyp.api_server.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 사용자 관리 컨트롤러
 * - 회원가입, 로그인 기능 제공
 */
@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 회원가입 API
     * @param signRequestDto 회원가입 요청 데이터 (email, password, userName)
     * @return 회원가입 성공/실패 메시지
     */
    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 사용자명으로 새 계정을 생성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "회원가입 성공",
            content = @Content(examples = @ExampleObject(value = "회원가입 완료"))),
        @ApiResponse(responseCode = "400", description = "회원가입 실패",
            content = @Content(examples = @ExampleObject(value = "이미 존재하는 이메일입니다.")))
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignRequestDto signRequestDto) {
        userService.signUp(signRequestDto);  // CustomException으로 예외 처리됨
        return ResponseEntity.ok("회원가입 완료");
    }

    /**
     * 로그인 API
     * @param loginRequestDto 로그인 요청 데이터 (email, password)
     * @return JWT 토큰 (accessToken, refreshToken) 또는 오류 메시지
     */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "로그인 실패",
            content = @Content(examples = {
                @ExampleObject(name = "존재하지 않는 사용자", value = "존재하지 않는 사용자입니다."),
                @ExampleObject(name = "비밀번호 불일치", value = "비밀번호가 일치하지 않습니다.")
            }))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        // CustomException으로 예외 처리됨 - GlobalExceptionHandler에서 일괄 처리
        TokenResponseDto tokenResponse = userService.login(loginRequestDto);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 토큰 갱신 API
     * @param request HTTP 요청 (Authorization 헤더에서 Refresh Token 추출)
     * @return 새로운 JWT 토큰 (accessToken, refreshToken)
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
            content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "토큰 갱신 실패",
            content = @Content(examples = {
                @ExampleObject(name = "만료된 토큰", value = "Refresh Token이 만료되었습니다."),
                @ExampleObject(name = "잘못된 토큰 타입", value = "유효하지 않은 토큰 타입입니다."),
                @ExampleObject(name = "토큰 없음", value = "토큰이 없습니다.")
            }))
    })
    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
        }
        
        String refreshToken = authHeader.substring(7);
        TokenResponseDto tokenResponse = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(tokenResponse);
    }
}
