package com.swyp.api_server.domain.alert.service;

import com.swyp.api_server.domain.alert.dto.AlertSettingRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingDetailRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingListResponseDTO;

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
     * 사용자의 상세 알림 설정 저장/업데이트
     */
    void saveDetailAlertSettings(String userEmail, String currencyCode, AlertSettingDetailRequestDTO detailSettings);
    
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
}