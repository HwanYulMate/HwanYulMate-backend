package com.swyp.api_server.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * FCM 중복 발송 방지 서비스
 * - Redis 기반 중복 알림 방지
 * - 목표 환율 알림: 하루 1회 제한
 * - 일일 환율 알림: 설정 시간에 1회만
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class FCMDuplicatePreventionService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String TARGET_RATE_PREFIX = "fcm:target_rate:";
    private static final String DAILY_RATE_PREFIX = "fcm:daily_rate:";
    private static final String FAILED_TOKEN_PREFIX = "fcm:failed_token:";
    
    /**
     * 목표 환율 알림 중복 발송 체크
     * @param userEmail 사용자 이메일
     * @param currencyCode 통화 코드
     * @param targetRate 목표 환율
     * @return 발송 가능하면 true, 중복이면 false
     */
    public boolean canSendTargetRateAlert(String userEmail, String currencyCode, double targetRate) {
        String key = TARGET_RATE_PREFIX + userEmail + ":" + currencyCode + ":" + targetRate;
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        String existingDate = redisTemplate.opsForValue().get(key);
        
        if (today.equals(existingDate)) {
            log.debug("목표 환율 알림 중복 방지: 사용자={}, 통화={}, 목표환율={}", userEmail, currencyCode, targetRate);
            return false;
        }
        
        // 오늘 날짜로 설정 (자정까지 유효)
        long secondsUntilMidnight = Duration.between(LocalDateTime.now(), 
                LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0)).getSeconds();
        
        redisTemplate.opsForValue().set(key, today, secondsUntilMidnight, TimeUnit.SECONDS);
        log.debug("목표 환율 알림 발송 허용: 사용자={}, 통화={}", userEmail, currencyCode);
        
        return true;
    }
    
    /**
     * 일일 환율 알림 중복 발송 체크
     * @param userEmail 사용자 이메일
     * @param currencyCode 통화 코드
     * @param alertTime 알림 시간 (HH:mm)
     * @return 발송 가능하면 true, 중복이면 false
     */
    public boolean canSendDailyRateAlert(String userEmail, String currencyCode, String alertTime) {
        String key = DAILY_RATE_PREFIX + userEmail + ":" + currencyCode + ":" + alertTime;
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        String existingDate = redisTemplate.opsForValue().get(key);
        
        if (today.equals(existingDate)) {
            log.debug("일일 환율 알림 중복 방지: 사용자={}, 통화={}, 시간={}", userEmail, currencyCode, alertTime);
            return false;
        }
        
        // 오늘 날짜로 설정 (자정까지 유효)
        long secondsUntilMidnight = Duration.between(LocalDateTime.now(), 
                LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0)).getSeconds();
        
        redisTemplate.opsForValue().set(key, today, secondsUntilMidnight, TimeUnit.SECONDS);
        log.debug("일일 환율 알림 발송 허용: 사용자={}, 통화={}, 시간={}", userEmail, currencyCode, alertTime);
        
        return true;
    }
    
    /**
     * FCM 전송 실패한 토큰 캐싱 (일정 기간 재시도 방지)
     * @param fcmToken 실패한 FCM 토큰
     * @param errorType 에러 타입
     */
    public void markTokenAsFailed(String fcmToken, String errorType) {
        String key = FAILED_TOKEN_PREFIX + fcmToken;
        String failureInfo = errorType + ":" + System.currentTimeMillis();
        
        // 에러 타입에 따른 차단 시간 설정
        long blockDurationMinutes = switch (errorType) {
            case "UNREGISTERED" -> 1440; // 24시간 (앱 삭제된 경우)
            case "INVALID_ARGUMENT" -> 360; // 6시간 (잘못된 토큰)
            case "QUOTA_EXCEEDED" -> 60; // 1시간 (할당량 초과)
            default -> 30; // 30분 (일반 에러)
        };
        
        redisTemplate.opsForValue().set(key, failureInfo, blockDurationMinutes, TimeUnit.MINUTES);
        log.info("FCM 토큰 실패 캐싱: 토큰={}, 에러={}, 차단시간={}분", fcmToken, errorType, blockDurationMinutes);
    }
    
    /**
     * FCM 토큰이 실패 목록에 있는지 확인
     * @param fcmToken 확인할 FCM 토큰
     * @return 차단된 토큰이면 true
     */
    public boolean isTokenBlocked(String fcmToken) {
        String key = FAILED_TOKEN_PREFIX + fcmToken;
        String failureInfo = redisTemplate.opsForValue().get(key);
        
        if (failureInfo != null) {
            log.debug("FCM 토큰 차단됨: {}", fcmToken);
            return true;
        }
        
        return false;
    }
    
    /**
     * 차단된 토큰 해제 (수동)
     * @param fcmToken 해제할 FCM 토큰
     */
    public void unblockToken(String fcmToken) {
        String key = FAILED_TOKEN_PREFIX + fcmToken;
        redisTemplate.delete(key);
        log.info("FCM 토큰 차단 해제: {}", fcmToken);
    }
    
    /**
     * 중복 방지 통계 조회
     */
    public java.util.Map<String, Long> getDuplicationStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        
        // 각 prefix별로 키 개수 조회
        java.util.Set<String> targetRateKeys = redisTemplate.keys(TARGET_RATE_PREFIX + "*");
        java.util.Set<String> dailyRateKeys = redisTemplate.keys(DAILY_RATE_PREFIX + "*");
        java.util.Set<String> failedTokenKeys = redisTemplate.keys(FAILED_TOKEN_PREFIX + "*");
        
        stats.put("blockedTargetRateAlerts", targetRateKeys != null ? (long) targetRateKeys.size() : 0L);
        stats.put("blockedDailyRateAlerts", dailyRateKeys != null ? (long) dailyRateKeys.size() : 0L);
        stats.put("failedTokensCount", failedTokenKeys != null ? (long) failedTokenKeys.size() : 0L);
        
        return stats;
    }
    
    /**
     * 중복 방지 캐시 정리 (개발/테스트용)
     */
    public void clearDuplicationCache() {
        java.util.Set<String> allKeys = redisTemplate.keys("fcm:*");
        if (allKeys != null && !allKeys.isEmpty()) {
            redisTemplate.delete(allKeys);
            log.info("FCM 중복 방지 캐시 정리 완료: {} 개 키 삭제", allKeys.size());
        }
    }
    
    /**
     * 만료된 캐시 수동 정리
     */
    public void cleanupExpiredCache() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        // 목표 환율 알림 캐시에서 오늘이 아닌 것들 정리
        java.util.Set<String> targetKeys = redisTemplate.keys(TARGET_RATE_PREFIX + "*");
        int cleanedCount = 0;
        
        if (targetKeys != null) {
            for (String key : targetKeys) {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null && !today.equals(value)) {
                    redisTemplate.delete(key);
                    cleanedCount++;
                }
            }
        }
        
        // 일일 환율 알림 캐시에서 오늘이 아닌 것들 정리
        java.util.Set<String> dailyKeys = redisTemplate.keys(DAILY_RATE_PREFIX + "*");
        if (dailyKeys != null) {
            for (String key : dailyKeys) {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null && !today.equals(value)) {
                    redisTemplate.delete(key);
                    cleanedCount++;
                }
            }
        }
        
        log.info("FCM 중복 방지 만료된 캐시 정리: {} 개 삭제", cleanedCount);
    }
}