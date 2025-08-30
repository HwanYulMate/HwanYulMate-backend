package com.swyp.api_server.domain.notification.service;

import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FCM 토큰 유효성 검증 및 자동 정리 서비스
 * - 무효한 토큰 자동 감지 및 제거
 * - 토큰 갱신 시 중복 방지
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class FCMTokenCleanupService {
    
    private final UserRepository userRepository;
    private final FCMService fcmService;
    
    // 토큰 유효성 검증 통계
    private long totalInvalidTokens = 0;
    private long totalClearedTokens = 0;
    
    /**
     * 주기적으로 무효한 FCM 토큰 정리 (매일 새벽 2시)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupInvalidTokens() {
        log.info("FCM 토큰 정리 작업 시작");
        
        List<User> usersWithTokens = userRepository.findByFcmTokenIsNotNull();
        if (usersWithTokens.isEmpty()) {
            log.info("FCM 토큰을 가진 사용자가 없습니다.");
            return;
        }
        
        int checkedCount = 0;
        int invalidCount = 0;
        
        for (User user : usersWithTokens) {
            if (isTokenInvalid(user.getFcmToken())) {
                log.warn("무효한 FCM 토큰 발견, 제거 중: 사용자={}, 토큰={}", 
                        user.getEmail(), user.getFcmToken());
                
                user.setFcmToken(null);
                userRepository.save(user);
                
                invalidCount++;
                totalInvalidTokens++;
            }
            checkedCount++;
        }
        
        totalClearedTokens += invalidCount;
        
        log.info("FCM 토큰 정리 작업 완료: 확인={} 개, 제거={} 개", checkedCount, invalidCount);
    }
    
    /**
     * FCM 토큰 유효성 검증
     * 테스트 알림을 보내서 토큰이 유효한지 확인
     */
    private boolean isTokenInvalid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return true;
        }
        
        try {
            // 조용한 테스트 알림 전송 (사용자에게는 보이지 않음)
            boolean success = fcmService.sendNotification(
                    token, 
                    "토큰 검증", 
                    "FCM 토큰 유효성 검증 중입니다.", 
                    java.util.Map.of("type", "TOKEN_VALIDATION", "silent", "true")
            );
            
            return !success; // 전송 실패 = 무효한 토큰
            
        } catch (Exception e) {
            log.warn("FCM 토큰 유효성 검증 중 오류: {}, 오류: {}", token, e.getMessage());
            return true; // 오류 발생 = 무효한 토큰으로 간주
        }
    }
    
    /**
     * 중복 FCM 토큰 정리
     * 동일한 토큰을 여러 사용자가 가지고 있는 경우 최신 사용자만 유지
     */
    @Scheduled(cron = "0 30 2 * * *") // 매일 새벽 2시 30분
    @Transactional
    public void removeDuplicateTokens() {
        log.info("중복 FCM 토큰 정리 작업 시작");
        
        List<String> duplicateTokens = userRepository.findDuplicateFcmTokens();
        if (duplicateTokens.isEmpty()) {
            log.info("중복된 FCM 토큰이 없습니다.");
            return;
        }
        
        int removedCount = 0;
        
        for (String token : duplicateTokens) {
            List<User> usersWithSameToken = userRepository.findByFcmToken(token);
            if (usersWithSameToken.size() > 1) {
                // 최신 사용자만 남기고 나머지는 토큰 제거
                usersWithSameToken.sort((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()));
                
                for (int i = 1; i < usersWithSameToken.size(); i++) {
                    User user = usersWithSameToken.get(i);
                    log.info("중복 FCM 토큰 제거: 사용자={}, 토큰={}", user.getEmail(), token);
                    
                    user.setFcmToken(null);
                    userRepository.save(user);
                    removedCount++;
                }
            }
        }
        
        log.info("중복 FCM 토큰 정리 완료: {} 개 제거", removedCount);
    }
    
    /**
     * FCM 토큰 정리 통계 조회
     */
    public java.util.Map<String, Long> getCleanupStatistics() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("totalInvalidTokens", totalInvalidTokens);
        stats.put("totalClearedTokens", totalClearedTokens);
        
        // 현재 유효한 토큰 수
        long currentValidTokens = userRepository.countByFcmTokenIsNotNull();
        stats.put("currentValidTokens", currentValidTokens);
        
        return stats;
    }
    
    /**
     * 통계 초기화
     */
    public void resetCleanupStatistics() {
        totalInvalidTokens = 0;
        totalClearedTokens = 0;
        log.info("FCM 토큰 정리 통계가 초기화되었습니다.");
    }
    
    /**
     * 정리 통계 로그 출력
     */
    public void logCleanupStatistics() {
        long currentValidTokens = userRepository.countByFcmTokenIsNotNull();
        
        log.info("=== FCM 토큰 정리 통계 ===");
        log.info("총 무효 토큰 발견: {} 개", totalInvalidTokens);
        log.info("총 정리된 토큰: {} 개", totalClearedTokens);
        log.info("현재 유효 토큰: {} 개", currentValidTokens);
        
        if (totalInvalidTokens > 0) {
            double cleanupRate = (double) totalClearedTokens / totalInvalidTokens * 100;
            log.info("정리 완료율: {:.1f}%", cleanupRate);
        }
    }
    
    /**
     * 특정 사용자의 FCM 토큰 유효성 즉시 검증
     * @param userEmail 사용자 이메일
     * @return 토큰이 유효하면 true, 무효하면 false (동시에 DB에서 제거)
     */
    @Transactional
    public boolean validateAndCleanupUserToken(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null || user.getFcmToken() == null) {
            return false;
        }
        
        boolean isValid = !isTokenInvalid(user.getFcmToken());
        
        if (!isValid) {
            log.info("사용자 FCM 토큰 무효로 인한 제거: 사용자={}", userEmail);
            user.setFcmToken(null);
            userRepository.save(user);
            totalInvalidTokens++;
            totalClearedTokens++;
        }
        
        return isValid;
    }
}