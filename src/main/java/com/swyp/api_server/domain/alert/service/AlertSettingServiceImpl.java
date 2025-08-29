package com.swyp.api_server.domain.alert.service;

import com.swyp.api_server.domain.alert.dto.AlertSettingRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingDetailRequestDTO;
import com.swyp.api_server.domain.alert.repository.AlertSettingRepository;
import com.swyp.api_server.domain.rate.service.ExchangeRateService;
import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.domain.notification.service.FCMService;
import com.swyp.api_server.entity.AlertSetting;
import com.swyp.api_server.entity.User;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * 알림 설정 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlertSettingServiceImpl implements AlertSettingService {
    
    private final AlertSettingRepository alertSettingRepository;
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;
    private final FCMService fcmService;
    
    @Override
    public void saveAlertSettings(String userEmail, List<AlertSettingRequestDTO> alertSettings) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + userEmail));
        
        for (AlertSettingRequestDTO setting : alertSettings) {
            // 기존 설정이 있는지 확인
            AlertSetting existingAlert = alertSettingRepository
                    .findByUserAndCurrencyCodeAndIsActiveTrue(user, setting.getName())
                    .orElse(null);
            
            if (existingAlert != null) {
                // 기존 설정 업데이트
                existingAlert.toggleActive();
                if (!setting.isEnabled()) {
                    existingAlert.toggleActive(); // 비활성화
                }
            } else if (setting.isEnabled()) {
                // 새로운 설정 생성
                AlertSetting newAlert = AlertSetting.builder()
                        .user(user)
                        .currencyCode(setting.getName())
                        .isActive(true)
                        .build();
                alertSettingRepository.save(newAlert);
            }
        }
        
        log.info("알림 설정 저장 완료: 사용자={}, 설정개수={}", userEmail, alertSettings.size());
    }
    
    @Override
    public void saveDetailAlertSettings(String userEmail, String currencyCode, AlertSettingDetailRequestDTO detailSettings) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + userEmail));
        
        // 기존 설정 조회 또는 새로 생성
        AlertSetting alertSetting = alertSettingRepository
                .findByUserAndCurrencyCodeAndIsActiveTrue(user, currencyCode)
                .orElse(AlertSetting.builder()
                        .user(user)
                        .currencyCode(currencyCode)
                        .isActive(true)
                        .build());
        
        // 상세 설정 업데이트
        LocalTime alertTime = detailSettings.getTodayExchangeRatePushTime() != null 
                ? LocalTime.parse(detailSettings.getTodayExchangeRatePushTime()) 
                : null;
        
        alertSetting.updateAlertSettings(
                detailSettings.isTargetPricePush(),
                BigDecimal.valueOf(detailSettings.getTargetPrice()),
                detailSettings.getTargetPricePushHow(),
                detailSettings.isTodayExchangeRatePush(),
                alertTime
        );
        
        alertSettingRepository.save(alertSetting);
        
        log.info("알림 상세 설정 저장: 사용자={}, 통화={}, 목표환율={}", 
                userEmail, currencyCode, detailSettings.getTargetPrice());
    }
    
    @Override
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void checkTargetPriceAchievement() {
        List<AlertSetting> targetAlerts = alertSettingRepository.findActiveTargetPriceAlerts();
        
        for (AlertSetting alert : targetAlerts) {
            try {
                // 현재 환율 조회
                var currentRate = exchangeRateService.getRealtimeExchangeRate(alert.getCurrencyCode());
                BigDecimal currentPrice = currentRate.getCurrentRate();
                
                // 목표 환율 달성 체크
                if (currentPrice.compareTo(alert.getTargetPrice()) <= 0) {
                    // 목표 달성 - 알림 발송
                    sendTargetPriceAlert(alert, currentPrice);
                    alert.markTargetAchieved();
                    alertSettingRepository.save(alert);
                }
                
            } catch (Exception e) {
                log.error("목표 환율 체크 중 오류: 사용자={}, 통화={}", 
                        alert.getUser().getEmail(), alert.getCurrencyCode(), e);
            }
        }
    }
    
    @Override
    @Scheduled(cron = "0 */1 * * * *") // 매 분마다 실행 (정확한 시간 체크)
    public void sendTodayExchangeRateAlerts() {
        LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);
        List<AlertSetting> dailyAlerts = alertSettingRepository.findTodayExchangeRateAlertsForTime(currentTime);
        
        for (AlertSetting alert : dailyAlerts) {
            try {
                // 현재 환율 조회
                var currentRate = exchangeRateService.getRealtimeExchangeRate(alert.getCurrencyCode());
                
                // 오늘의 환율 알림 발송
                sendDailyExchangeRateAlert(alert, currentRate);
                alert.updateLastDailyAlertSent();
                alertSettingRepository.save(alert);
                
            } catch (Exception e) {
                log.error("일일 환율 알림 발송 중 오류: 사용자={}, 통화={}", 
                        alert.getUser().getEmail(), alert.getCurrencyCode(), e);
            }
        }
    }
    
    /**
     * 목표 환율 달성 알림 발송
     */
    private void sendTargetPriceAlert(AlertSetting alert, BigDecimal currentPrice) {
        String message = String.format("%s가 목표 환율 %.2f원에 도달했습니다! (현재: %.2f원)",
                alert.getCurrencyCode(), 
                alert.getTargetPrice(),
                currentPrice);
        
        // 실제 푸시 알림 발송 로직 (FCM, APNs 등)
        log.info("목표 환율 달성 알림: {}", message);
        
        // FCM 푸시 알림 발송 (iOS 전용)
        if (alert.getUser().getFcmToken() != null) {
            fcmService.sendTargetRateAlert(
                alert.getUser().getFcmToken(),
                alert.getCurrencyCode(),
                alert.getTargetPrice().doubleValue(),
                currentPrice.doubleValue()
            );
            log.info("FCM 목표 환율 알림 전송: 사용자={}", alert.getUser().getEmail());
        } else {
            log.warn("FCM 토큰이 없어 알림을 전송할 수 없습니다: 사용자={}", alert.getUser().getEmail());
        }
    }
    
    /**
     * 오늘의 환율 알림 발송
     */
    private void sendDailyExchangeRateAlert(AlertSetting alert, 
            com.swyp.api_server.domain.rate.dto.response.ExchangeRealtimeResponseDTO currentRate) {
        String message = String.format("오늘 %s 환율: %.2f원 (전일 대비 %.2f%%)",
                currentRate.getCurrencyName(),
                currentRate.getCurrentRate(),
                currentRate.getChangeRate());
        
        // FCM 푸시 알림 발송
        log.info("오늘의 환율 알림: {}", message);
        
        if (alert.getUser().getFcmToken() != null) {
            fcmService.sendDailyRateAlert(
                alert.getUser().getFcmToken(),
                alert.getCurrencyCode(),
                currentRate.getCurrentRate().doubleValue(),
                currentRate.getPreviousRate().doubleValue()
            );
            log.info("FCM 일일 환율 알림 전송: 사용자={}", alert.getUser().getEmail());
        }
    }
}