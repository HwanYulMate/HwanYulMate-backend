package com.swyp.api_server.domain.user.controller;

import com.swyp.api_server.domain.user.dto.LoginRequestDto;
import com.swyp.api_server.domain.user.dto.SignRequestDto;
import com.swyp.api_server.domain.user.dto.TokenResponseDto;
import com.swyp.api_server.domain.user.dto.UserInfoResponseDto;
import com.swyp.api_server.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.swyp.api_server.common.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import com.swyp.api_server.common.util.AuthUtil;

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
    private final AuthUtil authUtil;

    /**
     * 회원가입 API
     * @param signRequestDto 회원가입 요청 데이터 (email, password, userName)
     * @return 회원가입 성공/실패 메시지
     */
    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 사용자명으로 새 계정을 생성합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "회원가입 성공",
            content = @Content(examples = @ExampleObject(value = "회원가입 완료"))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "회원가입 실패 (이미 존재하는 이메일 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
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
        @ApiResponse(
            responseCode = "200", 
            description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = TokenResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "로그인 실패 (존재하지 않는 사용자, 비밀번호 불일치 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
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
        @ApiResponse(
            responseCode = "200", 
            description = "토큰 갱신 성공",
            content = @Content(schema = @Schema(implementation = TokenResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "토큰 갱신 실패 (만료된 토큰, 잘못된 토큰 타입, 토큰 없음 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String refreshToken = authUtil.extractRefreshToken(request);
        TokenResponseDto tokenResponse = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 로그아웃 API
     * @param request HTTP 요청 (Authorization 헤더에서 Access Token 추출)
     * @return 로그아웃 성공 메시지
     */
    @Operation(summary = "로그아웃", description = "사용자의 Access Token을 무효화하여 로그아웃 처리합니다.",
               security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "로그아웃 성공",
            content = @Content(examples = @ExampleObject(value = "로그아웃 성공"))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "로그아웃 실패 (유효하지 않은 토큰, 토큰 타입 오류 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String accessToken = authUtil.extractAndValidateToken(request);
        authUtil.validateAccessToken(accessToken);
        userService.logout(accessToken);
        return ResponseEntity.ok("로그아웃 성공");
    }

    /**
     * 회원 탈퇴 API
     * @param request HTTP 요청 (Authorization 헤더에서 사용자 정보 추출)
     * @return 탈퇴 처리 결과 메시지
     */
    @Operation(summary = "회원 탈퇴", 
        description = "회원 탈퇴를 처리합니다. 즉시 삭제되지 않고 30일간 데이터가 보관된 후 완전 삭제됩니다.",
        security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "탈퇴 처리 성공",
            content = @Content(examples = @ExampleObject(value = "회원 탈퇴가 처리되었습니다. 30일 후 완전 삭제됩니다."))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "탈퇴 처리 실패 (이미 탈퇴 처리된 사용자 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패 (유효하지 않은 토큰 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "사용자 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/auth/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody(required = false) java.util.Map<String, String> withdrawRequest, HttpServletRequest request) {
        String email = authUtil.extractUserEmail(request);
        String reason = withdrawRequest != null ? withdrawRequest.get("reason") : null;
        userService.withdraw(email, reason);
        return ResponseEntity.ok("회원 탈퇴가 처리되었습니다. 30일 후 완전 삭제됩니다.");
    }

    /**
     * FCM 토큰 등록 API
     * @param fcmTokenRequest FCM 토큰 정보
     * @param request HTTP 요청
     * @return 등록 결과
     */
    @Operation(summary = "FCM 토큰 등록", description = "iOS 앱의 FCM 토큰을 등록하여 푸시 알림을 받을 수 있도록 설정합니다.",
               security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "FCM 토큰 등록 성공",
            content = @Content(examples = @ExampleObject(value = "FCM 토큰이 등록되었습니다."))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 (유효하지 않은 FCM 토큰 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/fcm/token")
    public ResponseEntity<?> registerFCMToken(@RequestBody java.util.Map<String, String> fcmTokenRequest, HttpServletRequest request) {
        String email = authUtil.extractUserEmail(request);
        String fcmToken = fcmTokenRequest.get("fcmToken");
        userService.updateFCMToken(email, fcmToken);
        return ResponseEntity.ok("FCM 토큰이 등록되었습니다.");
    }

    /**
     * 사용자 이름 변경 API
     * @param nameChangeRequest 변경할 이름 정보
     * @param request HTTP 요청
     * @return 변경 결과
     */
    @Operation(summary = "사용자 이름 변경", 
        description = "로그인한 사용자의 이름을 변경합니다. 소셜 로그인 사용자도 앱 내 표시명을 변경할 수 있습니다.",
        security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "이름 변경 성공",
            content = @Content(examples = @ExampleObject(value = "사용자 이름이 변경되었습니다."))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "이름 변경 실패 (빈 이름, 길이 초과, 동일한 이름 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "사용자 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/auth/profile/name")
    public ResponseEntity<?> updateUserName(@RequestBody java.util.Map<String, String> nameChangeRequest, HttpServletRequest request) {
        String email = authUtil.extractUserEmail(request);
        String newUserName = nameChangeRequest.get("userName");
        userService.updateUserName(email, newUserName);
        return ResponseEntity.ok("사용자 이름이 변경되었습니다.");
    }

    /**
     * 사용자 정보 조회 API
     * @param request HTTP 요청
     * @return 사용자 정보
     */
    @Operation(summary = "사용자 정보 조회", 
        description = "로그인한 사용자의 기본 정보를 조회합니다. (이메일, 이름, 가입일 등)",
        security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "사용자 정보 조회 성공",
            content = @Content(schema = @Schema(implementation = UserInfoResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "사용자 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/auth/profile")
    public ResponseEntity<?> getUserInfo(HttpServletRequest request) {
        String email = authUtil.extractUserEmail(request);
        UserInfoResponseDto userInfo = userService.getUserInfo(email);
        return ResponseEntity.ok(userInfo);
    }
}
