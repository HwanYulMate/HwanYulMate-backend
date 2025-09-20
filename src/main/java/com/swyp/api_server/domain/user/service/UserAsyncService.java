package com.swyp.api_server.domain.user.service;

import com.swyp.api_server.domain.auth.service.AppleTokenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 사용자 관련 비동기 처리 서비스
 * - 외부 API 호출을 트랜잭션과 분리
 * - Apple 토큰 무효화 등 외부 의존성 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAsyncService {
    
    private final AppleTokenValidator appleTokenValidator;
    
    /**
     * Apple 토큰 무효화 (비동기)
     * - 사용자 탈퇴 시 Apple 서버에 토큰 무효화 요청
     * - 실패해도 사용자 탈퇴는 정상 처리됨
     */
    @Async("externalApiExecutor")
    public CompletableFuture<Void> revokeAppleTokenAsync(String appleRefreshToken, String providerId, String userEmail) {
        try {
            log.info("Apple 토큰 무효화 시작 (비동기): {}", userEmail);
            
            if (appleRefreshToken != null && !appleRefreshToken.trim().isEmpty()) {
                // Apple refresh token 무효화
                appleTokenValidator.revokeAppleToken(appleRefreshToken, providerId);
                log.info("Apple refresh token 무효화 성공: {}", userEmail);
            }
            
            // Apple 계정 연동 해제
            appleTokenValidator.disconnectAppleAccount(providerId);
            log.info("Apple 계정 연동 해제 성공: {}", userEmail);
            
        } catch (Exception e) {
            // 외부 API 실패는 사용자 탈퇴에 영향을 주지 않음
            log.error("Apple 토큰 무효화 실패 (사용자 탈퇴는 정상 처리됨): {}", userEmail, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Apple 로그아웃 토큰 무효화 (비동기)
     * - 로그아웃 시 Apple 서버에 토큰 무효화 요청
     * - 실패해도 로그아웃은 정상 처리됨
     */
    @Async("externalApiExecutor")
    public CompletableFuture<Void> revokeAppleTokenForLogoutAsync(String appleRefreshToken, String providerId, String userEmail) {
        try {
            log.info("Apple 로그아웃 토큰 무효화 시작 (비동기): {}", userEmail);
            
            if (appleRefreshToken != null && !appleRefreshToken.trim().isEmpty()) {
                appleTokenValidator.revokeAppleToken(appleRefreshToken, providerId);
                log.info("Apple 로그아웃 토큰 무효화 성공: {}", userEmail);
            }
            
        } catch (Exception e) {
            // 외부 API 실패는 로그아웃에 영향을 주지 않음
            log.error("Apple 로그아웃 토큰 무효화 실패 (로그아웃은 정상 처리됨): {}", userEmail, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
}