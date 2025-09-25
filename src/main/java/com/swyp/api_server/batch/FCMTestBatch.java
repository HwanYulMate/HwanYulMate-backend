package com.swyp.api_server.batch;

import com.swyp.api_server.domain.notification.service.FCMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * FCM 푸시 알림 테스트 배치
 * - iOS 토큰으로 테스트 푸시 알림 전송
 * - 실행 방법: java -jar app.jar --spring.profiles.active=fcm-test
 */
@Component
@RequiredArgsConstructor
@Log4j2
@Profile("fcm-test")
public class FCMTestBatch implements CommandLineRunner {
    
    private final FCMService fcmService;
    
    // 테스트용 iOS 토큰 (실제 사용시 여기에 iOS에서 받은 토큰을 입력하세요)
    private static final String TEST_IOS_TOKEN = "YOUR_IOS_FCM_TOKEN_HERE";
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== FCM 푸시 알림 테스트 시작 ===");
        
        // 토큰 검증
        if (TEST_IOS_TOKEN.equals("YOUR_IOS_FCM_TOKEN_HERE")) {
            log.error("❌ 테스트 토큰을 설정해주세요!");
            log.info("FCMTestBatch.java 파일의 TEST_IOS_TOKEN 상수에 iOS에서 받은 FCM 토큰을 입력하세요.");
            return;
        }
        
        // 테스트 데이터 준비
        String testTitle = "🧪 FCM 테스트 알림";
        String testBody = "백엔드에서 Firebase를 통해 전송된 테스트 푸시 알림입니다.";
        
        Map<String, String> testData = new HashMap<>();
        testData.put("type", "TEST_NOTIFICATION");
        testData.put("testId", "test_" + System.currentTimeMillis());
        testData.put("source", "backend_batch");
        testData.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        log.info("📱 테스트 알림 전송 준비:");
        log.info("  - 제목: {}", testTitle);
        log.info("  - 내용: {}", testBody);
        log.info("  - 토큰: {}...{}", 
                TEST_IOS_TOKEN.substring(0, Math.min(20, TEST_IOS_TOKEN.length())),
                TEST_IOS_TOKEN.length() > 40 ? TEST_IOS_TOKEN.substring(TEST_IOS_TOKEN.length() - 20) : "");
        log.info("  - 추가 데이터: {}", testData);
        
        try {
            // FCM 전송 시도
            log.info("🚀 FCM 전송 시작...");
            boolean success = fcmService.sendNotification(TEST_IOS_TOKEN, testTitle, testBody, testData);
            
            if (success) {
                log.info("✅ FCM 테스트 알림이 성공적으로 전송되었습니다!");
                log.info("📱 iOS 기기에서 알림을 확인해보세요.");
            } else {
                log.error("❌ FCM 테스트 알림 전송에 실패했습니다.");
            }
            
        } catch (Exception e) {
            log.error("💥 FCM 전송 중 예외 발생: {}", e.getMessage(), e);
        }
        
        // 통계 출력
        log.info("📊 FCM 전송 통계:");
        fcmService.logStatistics();
        
        log.info("=== FCM 푸시 알림 테스트 완료 ===");
    }
}