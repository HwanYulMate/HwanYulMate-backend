package com.swyp.api_server.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터 - HTTP 요청의 Authorization 헤더에서 JWT 토큰을 추출하여 인증 처리
 * - 모든 HTTP 요청에 대해 한 번만 실행되는 필터
 * - Bearer 토큰 형식을 파싱하여 Security Context에 인증 정보 저장
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";  // Authorization 헤더 이름
    private static final String BEARER_PREFIX = "Bearer ";                // Bearer 토큰 접두사

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * HTTP 요청에서 JWT 토큰을 추출하여 인증 처리
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String jwt = resolveToken(request);  // 요청에서 JWT 토큰 추출
        
        // JWT 토큰이 존재하고, 유효하고, Access Token인 경우 인증 처리
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt) && jwtTokenProvider.isAccessToken(jwt)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);  // Security Context에 인증 정보 저장
            log.debug("Security Context에 '{}' 인증 정보를 저장했습니다.", authentication.getName());
        } else {
            log.debug("유효한 JWT 토큰이 없습니다.");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 JWT 토큰 추출
     * @param request HTTP 요청
     * @return JWT 토큰 문자열 (Bearer 접두사 제거된 순수 토큰)
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);  // Authorization 헤더 값 추출
        // "Bearer {token}" 형식인지 확인 후 토큰 부분만 추출
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;  // 올바르지 않은 형식이거나 헤더가 없는 경우
    }
}