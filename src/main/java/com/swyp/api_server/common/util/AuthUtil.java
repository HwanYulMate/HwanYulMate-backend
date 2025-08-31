package com.swyp.api_server.common.util;

import com.swyp.api_server.config.security.JwtTokenProvider;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 인증 관련 유틸리티 클래스
 * - JWT 토큰 검증 및 사용자 정보 추출 공통 로직
 */
@Component
@RequiredArgsConstructor
public class AuthUtil {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * HTTP 요청에서 JWT 토큰을 추출하고 검증
     * @param request HTTP 요청
     * @return 검증된 Access Token
     * @throws CustomException 토큰이 없거나 유효하지 않은 경우
     */
    public String extractAndValidateToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, 
                    "Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
        }
        
        String token = authHeader.substring(7);
        
        if (!jwtTokenProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 토큰입니다.");
        }
        
        return token;
    }
    
    /**
     * HTTP 요청에서 사용자 이메일 추출
     * @param request HTTP 요청
     * @return 사용자 이메일
     */
    public String extractUserEmail(HttpServletRequest request) {
        String token = extractAndValidateToken(request);
        return jwtTokenProvider.getEmailFromToken(token);
    }
    
    /**
     * Access Token 타입 검증
     * @param token JWT 토큰
     * @throws CustomException Access Token이 아닌 경우
     */
    public void validateAccessToken(String token) {
        if (!jwtTokenProvider.isAccessToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_TYPE, "Access Token이 아닙니다.");
        }
    }
    
    /**
     * Refresh Token 타입 검증
     * @param token JWT 토큰
     * @throws CustomException Refresh Token이 아닌 경우
     */
    public void validateRefreshToken(String token) {
        if (!jwtTokenProvider.isRefreshToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_TYPE, "유효하지 않은 토큰 타입입니다.");
        }
    }
    
    /**
     * HTTP 요청에서 Refresh Token 추출
     * @param request HTTP 요청
     * @return 추출된 Refresh Token
     * @throws CustomException 토큰이 없거나 형식이 올바르지 않은 경우
     */
    public String extractRefreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, 
                    "Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
        }
        
        return authHeader.substring(7);
    }
}