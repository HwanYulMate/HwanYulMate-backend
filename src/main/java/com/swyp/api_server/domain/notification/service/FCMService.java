package com.swyp.api_server.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * Firebase Cloud Messaging (FCM) 서비스
 * - iOS 앱으로 푸시 알림 전송
 */
@Service
@Log4j2
public class FCMService {

    /**
     * 단일 디바이스에 푸시 알림 전송
     * @param deviceToken iOS 앱의 FCM 토큰
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     */
    public void sendNotification(String deviceToken, String title, String body, java.util.Map<String, String> data) {
        try {
            if (deviceToken == null || deviceToken.trim().isEmpty()) {
                log.warn("디바이스 토큰이 없어서 푸시 알림을 전송할 수 없습니다.");
                return;
            }

            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(notification);
            
            // 추가 데이터가 있으면 포함
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            
            Message message = messageBuilder.build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 메시지 전송 성공: {}, 응답: {}", deviceToken, response);

        } catch (Exception e) {
            log.error("FCM 메시지 전송 실패: {}, 오류: {}", deviceToken, e.getMessage(), e);
        }
    }

    /**
     * 환율 알림 전송 (목표 환율 달성)
     * @param deviceToken 디바이스 토큰
     * @param currencyCode 통화 코드
     * @param targetRate 목표 환율
     * @param currentRate 현재 환율
     */
    public void sendTargetRateAlert(String deviceToken, String currencyCode, double targetRate, double currentRate) {
        String title = "🎯 목표 환율 달성!";
        String body = String.format("%s 환율이 목표가 %,.2f원에 도달했습니다. (현재: %,.2f원)", 
                currencyCode, targetRate, currentRate);
        
        java.util.Map<String, String> data = java.util.Map.of(
                "type", "TARGET_RATE_ACHIEVED",
                "currencyCode", currencyCode,
                "targetRate", String.valueOf(targetRate),
                "currentRate", String.valueOf(currentRate)
        );
        
        sendNotification(deviceToken, title, body, data);
    }

    /**
     * 일일 환율 알림 전송
     * @param deviceToken 디바이스 토큰
     * @param currencyCode 통화 코드
     * @param currentRate 현재 환율
     * @param previousRate 전일 환율
     */
    public void sendDailyRateAlert(String deviceToken, String currencyCode, double currentRate, double previousRate) {
        double changeRate = currentRate - previousRate;
        String changeIcon = changeRate > 0 ? "📈" : (changeRate < 0 ? "📉" : "➡️");
        
        String title = String.format("%s 📅 오늘의 환율", changeIcon);
        String body = String.format("%s: %,.2f원 (전일 대비 %+.2f원)", 
                currencyCode, currentRate, changeRate);
        
        java.util.Map<String, String> data = java.util.Map.of(
                "type", "DAILY_RATE_ALERT",
                "currencyCode", currencyCode,
                "currentRate", String.valueOf(currentRate),
                "previousRate", String.valueOf(previousRate),
                "changeRate", String.valueOf(changeRate)
        );
        
        sendNotification(deviceToken, title, body, data);
    }
}