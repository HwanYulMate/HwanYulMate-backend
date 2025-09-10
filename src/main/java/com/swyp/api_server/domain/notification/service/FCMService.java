package com.swyp.api_server.domain.notification.service;

import com.google.firebase.messaging.*;
import com.swyp.api_server.common.constants.Constants;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Firebase Cloud Messaging (FCM) 서비스
 * - iOS 앱으로 푸시 알림 전송
 * - 배치 처리 및 재시도 로직 포함
 */
@Service
@Log4j2
public class FCMService {
    
    // 통계용 카운터
    private long totalSentCount = 0;
    private long totalFailCount = 0;
    private long totalRetryCount = 0;

    /**
     * 단일 디바이스에 푸시 알림 전송 (재시도 로직 포함)
     * @param deviceToken iOS 앱의 FCM 토큰
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     * @return 전송 성공 여부
     */
    public boolean sendNotification(String deviceToken, String title, String body, java.util.Map<String, String> data) {
        if (deviceToken == null || deviceToken.trim().isEmpty()) {
            log.warn("디바이스 토큰이 없어서 푸시 알림을 전송할 수 없습니다.");
            return false;
        }

        return sendWithRetry(deviceToken, title, body, data, 0);
    }
    
    /**
     * 재시도 로직이 포함된 단일 알림 전송
     */
    private boolean sendWithRetry(String deviceToken, String title, String body, 
                                Map<String, String> data, int retryCount) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(notification);
            
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            
            Message message = messageBuilder.build();
            String response = FirebaseMessaging.getInstance().send(message);
            
            totalSentCount++;
            if (retryCount > 0) {
                totalRetryCount++;
                log.info("FCM 재시도 전송 성공: {}, 재시도={}, 응답={}", deviceToken, retryCount, response);
            } else {
                log.info("FCM 전송 성공: {}, 응답={}", deviceToken, response);
            }
            return true;
            
        } catch (FirebaseMessagingException e) {
            return handleFirebaseException(deviceToken, title, body, data, retryCount, e);
        } catch (Exception e) {
            log.error("FCM 전송 중 예상치 못한 오류: {}, 재시도={}, 오류={}", deviceToken, retryCount, e.getMessage());
            totalFailCount++;
            return false;
        }
    }
    
    /**
     * Firebase 예외 처리 및 재시도 판단
     */
    private boolean handleFirebaseException(String deviceToken, String title, String body,
                                          Map<String, String> data, int retryCount, 
                                          FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        
        // 재시도 불가능한 에러들
        if (errorCode == MessagingErrorCode.INVALID_ARGUMENT ||
            errorCode == MessagingErrorCode.UNREGISTERED) {
            log.warn("FCM 토큰 무효 (재시도 안함): {}, 에러코드={}", deviceToken, errorCode);
            totalFailCount++;
            return false;
        }
        
        // 재시도 가능한 에러들 (네트워크, 서버 오류 등)
        if (retryCount < Constants.Fcm.MAX_RETRY_COUNT && 
           (errorCode == MessagingErrorCode.UNAVAILABLE ||
            errorCode == MessagingErrorCode.INTERNAL ||
            errorCode == MessagingErrorCode.QUOTA_EXCEEDED)) {
            
            log.warn("FCM 전송 실패, 재시도 예정: {}, 에러코드={}, 재시도={}/{}", 
                    deviceToken, errorCode, retryCount + 1, Constants.Fcm.MAX_RETRY_COUNT);
            
            // 재시도 간격 (1초, 2초)
            try {
                Thread.sleep((retryCount + 1) * Constants.Fcm.RETRY_BASE_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            return sendWithRetry(deviceToken, title, body, data, retryCount + 1);
        }
        
        log.error("FCM 전송 최종 실패: {}, 에러코드={}, 재시도완료={}/{}", 
                deviceToken, errorCode, retryCount, Constants.Fcm.MAX_RETRY_COUNT);
        totalFailCount++;
        return false;
    }

    /**
     * 환율 알림 전송 (목표 환율 달성)
     * @param deviceToken 디바이스 토큰
     * @param currencyCode 통화 코드
     * @param targetRate 목표 환율
     * @param currentRate 현재 환율
     * @return 전송 성공 여부
     */
    public boolean sendTargetRateAlert(String deviceToken, String currencyCode, double targetRate, double currentRate) {
        String title = "목표 환율 달성";
        String body = String.format("%s %,.0f원 달성 (현재 %,.0f원)", 
                currencyCode, targetRate, currentRate);
        
        java.util.Map<String, String> data = java.util.Map.of(
                "type", Constants.Fcm.TARGET_RATE_ACHIEVED,
                "currencyCode", currencyCode,
                "targetRate", String.valueOf(targetRate),
                "currentRate", String.valueOf(currentRate)
        );
        
        return sendNotification(deviceToken, title, body, data);
    }

    /**
     * 일일 환율 알림 전송
     * @param deviceToken 디바이스 토큰
     * @param currencyCode 통화 코드
     * @param currentRate 현재 환율
     * @param previousRate 전일 환율
     * @return 전송 성공 여부
     */
    public boolean sendDailyRateAlert(String deviceToken, String currencyCode, double currentRate, double previousRate) {
        double changeRate = currentRate - previousRate;
        String changeText = changeRate > 0 ? "상승" : (changeRate < 0 ? "하락" : "보합");
        
        String title = "오늘의 환율";
        String body = String.format("%s %,.0f원 (%s %+.0f원)", 
                currencyCode, currentRate, changeText, Math.abs(changeRate));
        
        java.util.Map<String, String> data = java.util.Map.of(
                "type", Constants.Fcm.DAILY_RATE_ALERT,
                "currencyCode", currencyCode,
                "currentRate", String.valueOf(currentRate),
                "previousRate", String.valueOf(previousRate),
                "changeRate", String.valueOf(changeRate)
        );
        
        return sendNotification(deviceToken, title, body, data);
    }
    
    /**
     * 배치로 동일한 알림을 여러 디바이스에 전송
     * @param deviceTokens 디바이스 토큰 목록
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터
     * @return 성공한 토큰 개수
     */
    public int sendBatchNotification(List<String> deviceTokens, String title, String body, 
                                   Map<String, String> data) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.warn("전송할 디바이스 토큰이 없습니다.");
            return 0;
        }
        
        // 유효한 토큰만 필터링
        List<String> validTokens = deviceTokens.stream()
                .filter(token -> token != null && !token.trim().isEmpty())
                .collect(Collectors.toList());
        
        if (validTokens.isEmpty()) {
            log.warn("유효한 디바이스 토큰이 없습니다.");
            return 0;
        }
        
        int totalSuccessCount = 0;
        
        // 500개씩 배치로 나누어 전송
        for (int i = 0; i < validTokens.size(); i += Constants.Fcm.BATCH_SIZE) {
            int endIndex = Math.min(i + Constants.Fcm.BATCH_SIZE, validTokens.size());
            List<String> batchTokens = validTokens.subList(i, endIndex);
            
            int batchSuccessCount = sendBatchWithRetry(batchTokens, title, body, data, 0);
            totalSuccessCount += batchSuccessCount;
            
            log.info("배치 전송 완료: {}/{} 성공, 배치크기={}", 
                    batchSuccessCount, batchTokens.size(), batchTokens.size());
        }
        
        log.info("전체 배치 전송 완료: {}/{} 성공", totalSuccessCount, validTokens.size());
        return totalSuccessCount;
    }
    
    /**
     * 배치 전송 (재시도 로직 포함)
     */
    private int sendBatchWithRetry(List<String> deviceTokens, String title, String body,
                                 Map<String, String> data, int retryCount) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(deviceTokens)
                    .setNotification(notification);
            
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            
            MulticastMessage message = messageBuilder.build();
            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
            
            int successCount = response.getSuccessCount();
            int failureCount = response.getFailureCount();
            
            totalSentCount += successCount;
            totalFailCount += failureCount;
            
            if (retryCount > 0) {
                totalRetryCount += successCount;
                log.info("FCM 배치 재시도 완료: 성공={}, 실패={}, 재시도={}", 
                        successCount, failureCount, retryCount);
            } else {
                log.info("FCM 배치 전송 완료: 성공={}, 실패={}", successCount, failureCount);
            }
            
            // 실패한 토큰들에 대해 재시도 처리
            if (failureCount > 0 && retryCount < Constants.Fcm.MAX_RETRY_COUNT) {
                List<String> failedTokens = getFailedTokensForRetry(deviceTokens, response);
                if (!failedTokens.isEmpty()) {
                    log.warn("배치 전송 실패 토큰 재시도: {} 개, 재시도={}/{}", 
                            failedTokens.size(), retryCount + 1, Constants.Fcm.MAX_RETRY_COUNT);
                    
                    // 재시도 간격
                    Thread.sleep((retryCount + 1) * Constants.Fcm.RETRY_BASE_DELAY_MS);
                    
                    int retrySuccessCount = sendBatchWithRetry(failedTokens, title, body, data, retryCount + 1);
                    successCount += retrySuccessCount;
                }
            }
            
            return successCount;
            
        } catch (Exception e) {
            log.error("FCM 배치 전송 중 오류: 토큰수={}, 재시도={}, 오류={}", 
                    deviceTokens.size(), retryCount, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 재시도 가능한 실패 토큰들만 추출
     */
    private List<String> getFailedTokensForRetry(List<String> originalTokens, BatchResponse response) {
        List<String> retryTokens = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                FirebaseMessagingException exception = sendResponse.getException();
                MessagingErrorCode errorCode = exception.getMessagingErrorCode();
                
                // 재시도 가능한 에러인지 확인
                if (errorCode == MessagingErrorCode.UNAVAILABLE ||
                    errorCode == MessagingErrorCode.INTERNAL ||
                    errorCode == MessagingErrorCode.QUOTA_EXCEEDED) {
                    retryTokens.add(originalTokens.get(i));
                } else {
                    log.warn("재시도 불가능한 토큰: {}, 에러코드={}", 
                            originalTokens.get(i), errorCode);
                }
            }
        }
        
        return retryTokens;
    }
    
    /**
     * FCM 전송 통계 조회
     */
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalSent", totalSentCount);
        stats.put("totalFailed", totalFailCount);
        stats.put("totalRetry", totalRetryCount);
        
        double successRate = totalSentCount + totalFailCount > 0 ?
                (double) totalSentCount / (totalSentCount + totalFailCount) * 100 : 0;
        stats.put("successRate", (long) successRate);
        
        return stats;
    }
    
    /**
     * 통계 초기화 (주기적으로 호출)
     */
    public void resetStatistics() {
        totalSentCount = 0;
        totalFailCount = 0;
        totalRetryCount = 0;
        log.info("FCM 통계가 초기화되었습니다.");
    }
    
    /**
     * 통계 로그 출력
     */
    public void logStatistics() {
        if (totalSentCount + totalFailCount > 0) {
            double successRate = (double) totalSentCount / (totalSentCount + totalFailCount) * 100;
            log.info("=== FCM 전송 통계 ===");
            log.info("총 전송: {} 건", totalSentCount + totalFailCount);
            log.info("성공: {} 건", totalSentCount);
            log.info("실패: {} 건", totalFailCount);
            log.info("재시도: {} 건", totalRetryCount);
            log.info("성공률: {:.1f}%", successRate);
            
            // 실패율이 30% 이상이면 경고
            if (successRate < 70.0) {
                log.warn("⚠️ FCM 성공률이 낮습니다! 토큰 상태나 설정을 확인하세요.");
            }
        }
    }
}