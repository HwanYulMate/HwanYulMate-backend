package com.swyp.api_server.domain.notification.controller;

import com.swyp.api_server.domain.notification.service.FCMService;
import com.swyp.api_server.domain.notification.service.FCMStatisticsService;
import com.swyp.api_server.domain.notification.service.FCMTokenCleanupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FCM 관리 및 통계 조회 컨트롤러
 * - 개발/운영 모니터링용
 */
@RestController
@RequestMapping("/api/fcm/admin")
@RequiredArgsConstructor
@Tag(name = "FCM 관리", description = "FCM 알림 통계 및 관리 API")
public class FCMController {
    
    private final FCMService fcmService;
    private final FCMStatisticsService fcmStatisticsService;
    private final FCMTokenCleanupService fcmTokenCleanupService;
    
    /**
     * FCM 전송 통계 조회
     */
    @GetMapping("/statistics")
    @Operation(summary = "FCM 전송 통계 조회", description = "FCM 알림 전송 성공/실패/재시도 통계를 조회합니다")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        Map<String, Long> stats = fcmService.getStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * FCM 상태 요약 조회
     */
    @GetMapping("/status")
    @Operation(summary = "FCM 상태 요약", description = "현재 FCM 서비스 상태를 요약해서 조회합니다")
    public ResponseEntity<String> getStatus() {
        String status = fcmStatisticsService.getCurrentStatusSummary();
        return ResponseEntity.ok(status);
    }
    
    /**
     * FCM 건강 상태 체크
     */
    @GetMapping("/health")
    @Operation(summary = "FCM 건강 상태 체크", description = "FCM 서비스가 정상 작동하는지 확인합니다")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean isHealthy = fcmStatisticsService.isHealthy();
        Map<String, Object> health = Map.of(
                "healthy", isHealthy,
                "status", isHealthy ? "UP" : "DOWN",
                "service", "FCM"
        );
        return ResponseEntity.ok(health);
    }
    
    /**
     * FCM 문제 진단
     */
    @GetMapping("/diagnosis")
    @Operation(summary = "FCM 문제 진단", description = "FCM 서비스 문제점을 진단하고 해결 방안을 제시합니다")
    public ResponseEntity<String> diagnose() {
        String diagnosis = fcmStatisticsService.diagnoseProblems();
        return ResponseEntity.ok(diagnosis);
    }
    
    /**
     * FCM 토큰 정리 통계 조회
     */
    @GetMapping("/cleanup-stats")
    @Operation(summary = "FCM 토큰 정리 통계", description = "무효한 토큰 정리 작업 통계를 조회합니다")
    public ResponseEntity<Map<String, Long>> getCleanupStatistics() {
        Map<String, Long> stats = fcmTokenCleanupService.getCleanupStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 수동 토큰 정리 실행
     */
    @PostMapping("/cleanup")
    @Operation(summary = "수동 토큰 정리", description = "무효한 FCM 토큰을 수동으로 정리합니다")
    public ResponseEntity<String> manualCleanup() {
        fcmTokenCleanupService.cleanupInvalidTokens();
        return ResponseEntity.ok("FCM 토큰 정리 작업이 시작되었습니다.");
    }
    
    /**
     * 특정 사용자 토큰 검증
     */
    @PostMapping("/validate-token")
    @Operation(summary = "사용자 토큰 검증", description = "특정 사용자의 FCM 토큰을 검증합니다")
    public ResponseEntity<Map<String, Object>> validateUserToken(@RequestParam String userEmail) {
        boolean isValid = fcmTokenCleanupService.validateAndCleanupUserToken(userEmail);
        Map<String, Object> result = Map.of(
                "userEmail", userEmail,
                "tokenValid", isValid,
                "message", isValid ? "토큰이 유효합니다." : "토큰이 무효하거나 없습니다."
        );
        return ResponseEntity.ok(result);
    }
    
    /**
     * FCM 통계 초기화
     */
    @PostMapping("/reset-stats")
    @Operation(summary = "FCM 통계 초기화", description = "FCM 전송 통계를 초기화합니다")
    public ResponseEntity<String> resetStatistics() {
        fcmService.resetStatistics();
        fcmTokenCleanupService.resetCleanupStatistics();
        return ResponseEntity.ok("FCM 통계가 초기화되었습니다.");
    }
    
    /**
     * 테스트 알림 발송 (개발용)
     */
    @PostMapping("/test-notification")
    @Operation(summary = "테스트 알림 발송", description = "개발/테스트용 FCM 알림을 발송합니다")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @RequestParam String deviceToken,
            @RequestParam(defaultValue = "테스트 알림") String title,
            @RequestParam(defaultValue = "FCM 연결 테스트입니다.") String body) {
        
        Map<String, String> data = Map.of(
                "type", "TEST_NOTIFICATION",
                "timestamp", String.valueOf(System.currentTimeMillis())
        );
        
        boolean success = fcmService.sendNotification(deviceToken, title, body, data);
        
        Map<String, Object> result = Map.of(
                "success", success,
                "message", success ? "테스트 알림이 전송되었습니다." : "테스트 알림 전송에 실패했습니다.",
                "deviceToken", deviceToken
        );
        
        return ResponseEntity.ok(result);
    }
}