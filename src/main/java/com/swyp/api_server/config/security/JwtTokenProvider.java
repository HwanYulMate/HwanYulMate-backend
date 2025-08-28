package com.swyp.api_server.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 클래스
 * - Access Token과 Refresh Token 생성
 * - 토큰 유효성 검증
 * - 토큰에서 사용자 정보 추출
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;                              // JWT 서명에 사용할 암호화 키
    private final long accessTokenValidityInMilliseconds;     // Access Token 만료 시간 (밀리초)
    private final long refreshTokenValidityInMilliseconds;    // Refresh Token 만료 시간 (밀리초)

    /**
     * JWT 토큰 제공자 생성자
     * @param secretKey JWT 서명에 사용할 비밀키
     * @param accessTokenExpireTime Access Token 만료 시간 (밀리초)
     * @param refreshTokenExpireTime Refresh Token 만료 시간 (밀리초)
     */
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                           @Value("${jwt.access-token-expire-time}") long accessTokenExpireTime,
                           @Value("${jwt.refresh-token-expire-time}") long refreshTokenExpireTime) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMilliseconds = accessTokenExpireTime;
        this.refreshTokenValidityInMilliseconds = refreshTokenExpireTime;
    }

    /**
     * Access Token 생성
     * @param email 사용자 이메일 (JWT subject에 저장)
     * @param role 사용자 권한 (ROLE_USER, ROLE_ADMIN 등)
     * @return 생성된 Access Token 문자열
     */
    public String createAccessToken(String email, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(email)                    // 토큰 주체 (사용자 식별자)
                .claim("role", role)                  // 사용자 권한
                .claim("type", "access")              // 토큰 타입 구분
                .setIssuedAt(now)                     // 토큰 발급 시간
                .setExpiration(validity)              // 토큰 만료 시간
                .signWith(key, SignatureAlgorithm.HS256)  // 서명 알고리즘과 키
                .compact();
    }

    /**
     * Refresh Token 생성 (Access Token 갱신용)
     * @param email 사용자 이메일
     * @return 생성된 Refresh Token 문자열
     */
    public String createRefreshToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(email)                    // 토큰 주체
                .claim("type", "refresh")             // 토큰 타입 구분
                .setIssuedAt(now)                     // 토큰 발급 시간
                .setExpiration(validity)              // 토큰 만료 시간
                .signWith(key, SignatureAlgorithm.HS256)  // 서명
                .compact();
    }

    /**
     * JWT 토큰에서 Spring Security Authentication 객체 생성
     * @param token JWT 토큰 문자열
     * @return Spring Security Authentication 객체
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String email = claims.getSubject();                    // 토큰에서 이메일 추출
        String role = claims.get("role", String.class);       // 토큰에서 권한 추출
        
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
        return new UsernamePasswordAuthenticationToken(email, "", Collections.singletonList(authority));
    }

    /**
     * JWT 토큰에서 사용자 이메일 추출
     * @param token JWT 토큰 문자열
     * @return 사용자 이메일
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * JWT 토큰 유효성 검증
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());  // 만료 시간 확인
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Access Token 타입 여부 확인
     * @param token 확인할 JWT 토큰
     * @return Access Token이면 true
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "access".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Refresh Token 타입 여부 확인
     * @param token 확인할 JWT 토큰
     * @return Refresh Token이면 true
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * JWT 토큰을 파싱하여 Claims 정보 추출
     * @param token 파싱할 JWT 토큰
     * @return JWT Claims 정보
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(key)               // 서명 검증용 키 설정
                .build()
                .parseClaimsJws(token)            // 토큰 파싱 및 서명 검증
                .getBody();                       // Claims 반환
    }
}