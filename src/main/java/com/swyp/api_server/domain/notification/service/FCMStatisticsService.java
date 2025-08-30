package com.swyp.api_server.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * FCM 알림 발송 통계 및 로깅 서비스
 * - 주기적인 통계 로그 출력
 * - 성능 모니터링 및 경고
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class FCMStatisticsService {
    
    private final FCMService fcmService;
    private final FCMTokenCleanupService fcmTokenCleanupService;
    
    /**
     * FCM 통계 주기적 로그 출력 (매 시간마다)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void logHourlyStatistics() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        log.info("=== FCM 시간별 통계 ({}) ===", timestamp);
        
        // FCM 전송 통계
        Map<String, Long> fcmStats = fcmService.getStatistics();
        log.info("📊 전송 통계: 성공={}, 실패={}, 재시도={}, 성공률={}%", 
                fcmStats.get("totalSent"),
                fcmStats.get("totalFailed"), 
                fcmStats.get("totalRetry"),
                fcmStats.get("successRate"));
        
        // 토큰 정리 통계
        Map<String, Long> cleanupStats = fcmTokenCleanupService.getCleanupStatistics();
        log.info("🧹 토큰 정리: 현재 유효={}, 총 정리={}, 무효 발견={}", 
                cleanupStats.get("currentValidTokens"),
                cleanupStats.get("totalClearedTokens"),
                cleanupStats.get("totalInvalidTokens"));
        
        // 경고 체크
        checkAndWarnIfNeeded(fcmStats, cleanupStats);
    }
    
    /**
     * FCM 통계 일일 요약 로그 (매일 자정)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void logDailyStatistics() {
        String date = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("=== FCM 일일 통계 요약 ({}) ===", date);
        
        // FCM 서비스 통계 출력
        fcmService.logStatistics();
        
        // 토큰 정리 서비스 통계 출력
        fcmTokenCleanupService.logCleanupStatistics();
        
        // 하루가 지났으므로 통계 초기화
        fcmService.resetStatistics();
        fcmTokenCleanupService.resetCleanupStatistics();
        
        log.info("일일 통계 초기화 완료");
    }
    
    /**
     * 성능 및 상태 경고 체크
     */
    private void checkAndWarnIfNeeded(Map<String, Long> fcmStats, Map<String, Long> cleanupStats) {
        long successRate = fcmStats.get("successRate");
        long totalSent = fcmStats.get("totalSent");
        long totalFailed = fcmStats.get("totalFailed");
        long totalInvalidTokens = cleanupStats.get("totalInvalidTokens");
        long currentValidTokens = cleanupStats.get("currentValidTokens");
        
        // FCM 성공률이 낮은 경우 경고
        if (successRate < 80 && (totalSent + totalFailed) > 10) {
            log.warn("🚨 FCM 성공률이 낮습니다! 성공률: {}%, Firebase 콘솔을 확인하세요.", successRate);
        }
        
        // 무효한 토큰이 많은 경우 경고
        if (totalInvalidTokens > 0 && currentValidTokens > 0) {
            double invalidRate = (double) totalInvalidTokens / (totalInvalidTokens + currentValidTokens) * 100;
            if (invalidRate > 20) {
                log.warn("🚨 무효한 FCM 토큰 비율이 높습니다! 무효 토큰: {}%, 앱 버전을 확인하세요.", 
                        String.format("%.1f", invalidRate));
            }
        }
        
        // 전송량이 비정상적으로 많은 경우 경고
        if (totalSent > 1000) {
            log.warn("⚠️ 시간당 FCM 전송량이 높습니다: {} 건, 스팸 가능성을 확인하세요.", totalSent);
        }
        
        // 유효한 토큰이 없는 경우 경고
        if (currentValidTokens == 0) {
            log.warn("⚠️ 유효한 FCM 토큰이 없습니다. 사용자가 앱을 설치했는지 확인하세요.");
        }
    }
    
    /**
     * 현재 FCM 상태 요약 조회
     */
    public String getCurrentStatusSummary() {
        Map<String, Long> fcmStats = fcmService.getStatistics();
        Map<String, Long> cleanupStats = fcmTokenCleanupService.getCleanupStatistics();
        
        StringBuilder summary = new StringBuilder();
        summary.append("📱 FCM 상태 요약\n");
        summary.append(String.format("• 유효 토큰: %d 개\n", cleanupStats.get("currentValidTokens")));
        summary.append(String.format("• 전송 성공률: %d%%\n", fcmStats.get("successRate")));
        summary.append(String.format("• 총 전송: %d 건 (성공: %d, 실패: %d)\n", 
                fcmStats.get("totalSent") + fcmStats.get("totalFailed"),
                fcmStats.get("totalSent"), 
                fcmStats.get("totalFailed")));
        summary.append(String.format("• 재시도: %d 건\n", fcmStats.get("totalRetry")));
        summary.append(String.format("• 정리된 토큰: %d 개", cleanupStats.get("totalClearedTokens")));
        
        return summary.toString();
    }
    
    /**
     * FCM 건강 상태 체크
     */
    public boolean isHealthy() {
        Map<String, Long> fcmStats = fcmService.getStatistics();
        Map<String, Long> cleanupStats = fcmTokenCleanupService.getCleanupStatistics();
        
        long successRate = fcmStats.get("successRate");
        long totalTransactions = fcmStats.get("totalSent") + fcmStats.get("totalFailed");
        long currentValidTokens = cleanupStats.get("currentValidTokens");
        
        // 건강 상태 판단 기준:
        // 1. 성공률이 70% 이상
        // 2. 유효한 토큰이 1개 이상 존재
        // 3. 전송이 아예 없거나, 있다면 성공률이 기준치 이상
        
        boolean hasValidTokens = currentValidTokens > 0;
        boolean goodSuccessRate = totalTransactions == 0 || successRate >= 70;
        
        return hasValidTokens && goodSuccessRate;
    }
    
    /**
     * 문제 상황 진단 및 해결 방안 제시
     */
    public String diagnoseProblems() {
        Map<String, Long> fcmStats = fcmService.getStatistics();
        Map<String, Long> cleanupStats = fcmTokenCleanupService.getCleanupStatistics();
        
        StringBuilder diagnosis = new StringBuilder();
        diagnosis.append("🔍 FCM 문제 진단\n");
        
        long successRate = fcmStats.get("successRate");
        long totalSent = fcmStats.get("totalSent");
        long totalFailed = fcmStats.get("totalFailed");
        long currentValidTokens = cleanupStats.get("currentValidTokens");
        
        if (currentValidTokens == 0) {
            diagnosis.append("❌ 유효한 FCM 토큰이 없습니다.\n");
            diagnosis.append("   → iOS 앱에서 FCM 토큰을 서버로 전송했는지 확인\n");
            diagnosis.append("   → Firebase 프로젝트 설정 확인\n");
        }
        
        if (successRate < 70 && (totalSent + totalFailed) > 5) {
            diagnosis.append(String.format("❌ FCM 성공률이 낮습니다 (%d%%)\n", successRate));
            diagnosis.append("   → Firebase 서비스 계정 키 파일 확인\n");
            diagnosis.append("   → APNs 설정 확인 (iOS)\n");
            diagnosis.append("   → 네트워크 연결 상태 확인\n");
        }
        
        if (totalFailed > totalSent) {
            diagnosis.append("❌ 실패 전송이 성공 전송보다 많습니다.\n");
            diagnosis.append("   → 대량의 무효한 토큰 존재 가능성\n");
            diagnosis.append("   → FCM 토큰 정리 작업 확인\n");
        }
        
        if (diagnosis.toString().equals("🔍 FCM 문제 진단\n")) {
            diagnosis.append("✅ 현재 특별한 문제가 발견되지 않았습니다.");
        }
        
        return diagnosis.toString();
    }
}