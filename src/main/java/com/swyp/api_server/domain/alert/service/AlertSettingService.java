package com.swyp.api_server.domain.alert.service;

import com.swyp.api_server.domain.alert.dto.AlertSettingRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingListResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertTargetRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertTargetResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertDailyRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertDailyResponseDTO;

import java.util.List;

/**
 * 알림 설정 서비스 인터페이스
 */
public interface AlertSettingService {
    
    /**
     * 사용자의 통화별 알림 설정 저장/업데이트
     */
    void saveAlertSettings(String userEmail, List<AlertSettingRequestDTO> alertSettings);
    
    
    /**
     * 사용자의 전체 알림 설정 조회
     */
    List<AlertSettingResponseDTO> getAllAlertSettings(Long userId);
    
    /**
     * 특정 통화의 알림 설정 조회
     */
    AlertSettingResponseDTO getAlertSetting(Long userId, String currencyCode);
    
    /**
     * 목표 환율 달성 체크 및 알림 발송
     */
    void checkTargetPriceAchievement();
    
    /**
     * 오늘의 환율 알림 발송
     */
    void sendTodayExchangeRateAlerts();
    
    /**
     * 목표 환율 알림 설정 저장/업데이트
     */
    void saveTargetAlertSettings(String userEmail, String currencyCode, AlertTargetRequestDTO targetSettings);
    
    /**
     * 일일 환율 알림 설정 저장/업데이트
     */
    void saveDailyAlertSettings(String userEmail, String currencyCode, AlertDailyRequestDTO dailySettings);
    
    /**
     * 목표 환율 알림 설정 조회
     */
    AlertTargetResponseDTO getTargetAlertSetting(Long userId, String currencyCode);
    
    /**
     * 일일 환율 알림 설정 조회
     */
    AlertDailyResponseDTO getDailyAlertSetting(Long userId, String currencyCode);
}