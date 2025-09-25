package com.swyp.api_server.controller;

import com.swyp.api_server.domain.notification.service.FCMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * FCM 푸시 알림 테스트 컨트롤러
 * - REST API로 푸시 알림 테스트
 * - 프로필: fcm-test 또는 dev 환경에서만 활성화
 */
@RestController
@RequestMapping("/api/test/fcm")
@RequiredArgsConstructor
@Log4j2
@Profile({"fcm-test", "dev", "local", "default"})
public class FCMTestController {
    
    private final FCMService fcmService;
    
    /**
     * FCM 테스트 푸시 알림 전송
     * POST /api/test/fcm/send
     * 
     * 요청 예시:
     * {
     *   "token": "iOS에서 받은 FCM 토큰",
     *   "title": "테스트 제목",
     *   "body": "테스트 내용"
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendTestNotification(@RequestBody FCMTestRequest request) {
        log.info("=== FCM 테스트 푸시 알림 전송 ===");
        
        // 요청 검증
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("success", false, "message", "FCM 토큰이 필요합니다.")
            );
        }
        
        String title = request.getTitle() != null ? request.getTitle() : "🧪 FCM 테스트 알림";
        String body = request.getBody() != null ? request.getBody() : "백엔드에서 Firebase를 통해 전송된 테스트 푸시 알림입니다.";
        
        // 추가 데이터 설정
        Map<String, String> data = new HashMap<>();
        data.put("type", "TEST_NOTIFICATION");
        data.put("testId", "test_" + System.currentTimeMillis());
        data.put("source", "backend_api");
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        log.info("📱 테스트 알림 전송:");
        log.info("  - 제목: {}", title);
        log.info("  - 내용: {}", body);
        log.info("  - 토큰: {}...{}", 
                request.getToken().substring(0, Math.min(20, request.getToken().length())),
                request.getToken().length() > 40 ? 
                    request.getToken().substring(request.getToken().length() - 20) : "");
        log.info("  - 추가 데이터: {}", data);
        
        try {
            // FCM 전송 시도
            log.info("🚀 FCM 전송 시작...");
            boolean success = fcmService.sendNotification(request.getToken(), title, body, data);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("token", request.getToken().substring(0, Math.min(20, request.getToken().length())) + "...");
            response.put("title", title);
            response.put("body", body);
            response.put("data", data);
            response.put("timestamp", System.currentTimeMillis());
            
            if (success) {
                log.info("✅ FCM 테스트 알림이 성공적으로 전송되었습니다!");
                response.put("message", "푸시 알림이 성공적으로 전송되었습니다. iOS 기기에서 확인해보세요.");
            } else {
                log.error("❌ FCM 테스트 알림 전송에 실패했습니다.");
                response.put("message", "푸시 알림 전송에 실패했습니다. 토큰과 설정을 확인해주세요.");
            }
            
            // 통계 출력
            fcmService.logStatistics();
            response.put("statistics", fcmService.getStatistics());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("💥 FCM 전송 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of(
                    "success", false, 
                    "message", "서버 오류로 인해 푸시 알림 전송에 실패했습니다: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
                )
            );
        }
    }
    
    /**
     * FCM 전송 통계 조회
     * GET /api/test/fcm/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("FCM 통계 조회 요청");
        
        Map<String, Long> stats = fcmService.getStatistics();
        Map<String, Object> response = new HashMap<>();
        response.put("statistics", stats);
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("📊 현재 FCM 통계: {}", stats);
        fcmService.logStatistics();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * FCM 통계 초기화
     * DELETE /api/test/fcm/statistics
     */
    @DeleteMapping("/statistics")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        log.info("FCM 통계 초기화 요청");
        
        fcmService.resetStatistics();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "FCM 통계가 초기화되었습니다.");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * FCM 테스트 요청 DTO
     */
    public static class FCMTestRequest {
        private String token;
        private String title;
        private String body;
        
        // Getters and Setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
}