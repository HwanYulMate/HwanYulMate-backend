package com.swyp.api_server.domain.alert.service;

import com.swyp.api_server.domain.alert.dto.AlertSettingRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingListResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertTargetRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertTargetResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertDailyRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertDailyResponseDTO;
import com.swyp.api_server.domain.alert.repository.AlertSettingRepository;
import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.domain.rate.service.ExchangeRateService;
import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.domain.notification.service.FCMService;
import com.swyp.api_server.domain.common.service.DistributedLockService;
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
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final DistributedLockService distributedLockService;
    
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
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void checkTargetPriceAchievement() {
        // 분산 락으로 중복 실행 방지
        String instanceId = java.util.UUID.randomUUID().toString();
        if (!distributedLockService.trySchedulerLock("targetPriceCheck", instanceId)) {
            log.debug("목표 환율 체크 스케줄러가 다른 인스턴스에서 실행 중입니다.");
            return;
        }
        
        try {
        List<AlertSetting> targetAlerts = alertSettingRepository.findActiveTargetPriceAlertsWithValidTokens();
        
        if (targetAlerts.isEmpty()) {
            log.debug("목표 환율 알림 대상이 없습니다.");
            return;
        }
        
        log.info("목표 환율 체크 시작: {} 개 알림 대상", targetAlerts.size());
        int successCount = 0;
        int failCount = 0;
        
        for (AlertSetting alert : targetAlerts) {
            try {
                // 현재 환율 조회
                var currentRate = exchangeRateService.getRealtimeExchangeRate(alert.getCurrencyCode());
                BigDecimal currentPrice = currentRate.getCurrentRate();
                
                // 목표 환율 달성 체크
                if (currentPrice.compareTo(alert.getTargetPrice()) <= 0) {
                    // 목표 달성 - 알림 발송
                    boolean sent = sendTargetPriceAlert(alert, currentPrice);
                    if (sent) {
                        alert.markTargetAchieved();
                        alertSettingRepository.save(alert);
                        successCount++;
                        log.info("목표 환율 알림 발송 성공: 사용자={}, 통화={}, 목표={}, 현재={}", 
                                alert.getUser().getEmail(), alert.getCurrencyCode(), 
                                alert.getTargetPrice(), currentPrice);
                    } else {
                        failCount++;
                        log.warn("목표 환율 알림 발송 실패: 사용자={}", alert.getUser().getEmail());
                    }
                }
                
            } catch (Exception e) {
                failCount++;
                log.error("목표 환율 체크 중 오류: 사용자={}, 통화={}", 
                        alert.getUser().getEmail(), alert.getCurrencyCode(), e);
            }
        }
        
        if (successCount > 0 || failCount > 0) {
            log.info("목표 환율 체크 완료: 성공={}, 실패={}", successCount, failCount);
        }
        
        } finally {
            // 스케줄러 락 해제
            distributedLockService.releaseSchedulerLock("targetPriceCheck", instanceId);
        }
    }
    
    @Override
    @Scheduled(cron = "0 */1 * * * *") // 매 분마다 실행 (정확한 시간 체크)
    public void sendTodayExchangeRateAlerts() {
        // 분산 락으로 중복 실행 방지
        String instanceId = java.util.UUID.randomUUID().toString();
        if (!distributedLockService.trySchedulerLock("dailyExchangeRateAlerts", instanceId)) {
            return; // 조용히 종료
        }
        
        try {
        LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);
        List<AlertSetting> dailyAlerts = alertSettingRepository.findTodayExchangeRateAlertsForTimeWithValidTokens(currentTime);
        
        if (dailyAlerts.isEmpty()) {
            return; // 알림 대상이 없으면 조용히 종료
        }
        
        log.info("일일 환율 알림 발송 시작: {} 시 {} 분, {} 개 대상", 
                currentTime.getHour(), currentTime.getMinute(), dailyAlerts.size());
        int successCount = 0;
        int failCount = 0;
        
        for (AlertSetting alert : dailyAlerts) {
            try {
                // 현재 환율 조회
                var currentRate = exchangeRateService.getRealtimeExchangeRate(alert.getCurrencyCode());
                
                // 오늘의 환율 알림 발송
                boolean sent = sendDailyExchangeRateAlert(alert, currentRate);
                if (sent) {
                    alert.updateLastDailyAlertSent();
                    alertSettingRepository.save(alert);
                    successCount++;
                    log.info("일일 환율 알림 발송 성공: 사용자={}, 통화={}", 
                            alert.getUser().getEmail(), alert.getCurrencyCode());
                } else {
                    failCount++;
                    log.warn("일일 환율 알림 발송 실패: 사용자={}", alert.getUser().getEmail());
                }
                
            } catch (Exception e) {
                failCount++;
                log.error("일일 환율 알림 발송 중 오류: 사용자={}, 통화={}", 
                        alert.getUser().getEmail(), alert.getCurrencyCode(), e);
            }
        }
        
        log.info("일일 환율 알림 발송 완료: 성공={}, 실패={}", successCount, failCount);
        
        } finally {
            // 스케줄러 락 해제
            distributedLockService.releaseSchedulerLock("dailyExchangeRateAlerts", instanceId);
        }
    }
    
    /**
     * 목표 환율 달성 알림 발송
     * @return 알림 발송 성공 여부
     */
    private boolean sendTargetPriceAlert(AlertSetting alert, BigDecimal currentPrice) {
        // FCM 푸시 알림 발송 (iOS 전용)
        if (alert.getUser().getFcmToken() != null) {
            boolean success = fcmService.sendTargetRateAlert(
                alert.getUser().getFcmToken(),
                alert.getCurrencyCode(),
                alert.getTargetPrice().doubleValue(),
                currentPrice.doubleValue()
            );
            
            if (success) {
                log.debug("FCM 목표 환율 알림 전송 성공: 사용자={}", alert.getUser().getEmail());
            } else {
                log.error("FCM 목표 환율 알림 전송 실패: 사용자={}", alert.getUser().getEmail());
            }
            
            return success;
        } else {
            log.warn("FCM 토큰이 없어 알림을 전송할 수 없습니다: 사용자={}", alert.getUser().getEmail());
            return false;
        }
    }
    
    /**
     * 오늘의 환율 알림 발송
     * @return 알림 발송 성공 여부
     */
    private boolean sendDailyExchangeRateAlert(AlertSetting alert, 
            com.swyp.api_server.domain.rate.dto.response.ExchangeRealtimeResponseDTO currentRate) {
        
        if (alert.getUser().getFcmToken() != null) {
            boolean success = fcmService.sendDailyRateAlert(
                alert.getUser().getFcmToken(),
                alert.getCurrencyCode(),
                currentRate.getCurrentRate().doubleValue(),
                currentRate.getPreviousRate().doubleValue()
            );
            
            if (success) {
                log.debug("FCM 일일 환율 알림 전송 성공: 사용자={}", alert.getUser().getEmail());
            } else {
                log.error("FCM 일일 환율 알림 전송 실패: 사용자={}", alert.getUser().getEmail());
            }
            
            return success;
        } else {
            log.warn("FCM 토큰이 없어 일일 환율 알림을 전송할 수 없습니다: 사용자={}", alert.getUser().getEmail());
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AlertSettingResponseDTO> getAllAlertSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 ID: " + userId));
        
        // 기존 알림 설정을 Map으로 변환
        List<AlertSetting> userAlertSettings = alertSettingRepository.findByUserAndIsActiveTrue(user);
        java.util.Map<String, AlertSetting> alertSettingsMap = userAlertSettings.stream()
                .collect(Collectors.toMap(AlertSetting::getCurrencyCode, setting -> setting));
        
        List<AlertSettingResponseDTO> result = new java.util.ArrayList<>();
        
        // 모든 ExchangeType에 대해 일관된 순서로 응답 생성
        for (ExchangeList.ExchangeType exchangeType : ExchangeList.ExchangeType.values()) {
            String currencyCode = exchangeType.getCode();
            AlertSetting existingSetting = alertSettingsMap.get(currencyCode);
            
            if (existingSetting != null) {
                // 기존 설정이 있는 경우
                result.add(convertToResponseDTO(existingSetting));
            } else {
                // 설정이 없는 경우 기본값으로 생성
                AlertSettingResponseDTO defaultSetting = createDefaultAlertSettingDTO(exchangeType);
                result.add(defaultSetting);
            }
        }
        
        log.info("알림 설정 조회 완료: 사용자={}, 총 {} 개 통화", user.getEmail(), result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AlertSettingResponseDTO getAlertSetting(Long userId, String currencyCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 ID: " + userId));
        
        // 지원하는 통화인지 먼저 확인
        ExchangeList.ExchangeType exchangeType;
        try {
            exchangeType = ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "지원하지 않는 통화 코드입니다: " + currencyCode);
        }
        
        Optional<AlertSetting> alertSetting = alertSettingRepository.findByUserAndCurrencyCodeAndIsActiveTrue(user, currencyCode);
        if (alertSetting.isPresent()) {
            return convertToResponseDTO(alertSetting.get());
        } else {
            // 설정이 없으면 기본값 반환
            log.info("알림 설정이 없어 기본값 반환: 사용자={}, 통화={}", user.getEmail(), currencyCode);
            return createDefaultAlertSettingDTO(exchangeType);
        }
    }

    private AlertSettingResponseDTO convertToResponseDTO(AlertSetting alertSetting) {
        String currencyCode = alertSetting.getCurrencyCode();
        
        // ExchangeList에서 통화 정보 조회
        String currencyName = getCurrencyName(currencyCode);
        String flagImageUrl = getFlagImageUrl(currencyCode);
        
        return new AlertSettingResponseDTO(
                currencyCode,
                currencyName,
                flagImageUrl,
                alertSetting.getTargetPricePush(),
                alertSetting.getTodayExchangeRatePush(),
                alertSetting.getTargetPrice(),
                alertSetting.getTargetPricePushHow(),
                alertSetting.getTodayExchangeRatePushTime() != null ? 
                        alertSetting.getTodayExchangeRatePushTime().toString() : null
        );
    }
    
    /**
     * 통화 코드에 해당하는 한글 이름 조회
     */
    private String getCurrencyName(String currencyCode) {
        try {
            return ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase()).getLabel();
        } catch (IllegalArgumentException e) {
            return currencyCode; // 찾을 수 없으면 코드 그대로 반환
        }
    }
    
    /**
     * 통화 코드에 해당하는 국기 이미지 URL 조회
     */
    private String getFlagImageUrl(String currencyCode) {
        try {
            return ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase()).getFlagImageUrl();
        } catch (IllegalArgumentException e) {
            return "/images/flags/default.png"; // 기본 이미지 반환
        }
    }
    
    /**
     * 알림 설정이 없는 통화에 대한 기본값 생성
     */
    private AlertSettingResponseDTO createDefaultAlertSettingDTO(ExchangeList.ExchangeType exchangeType) {
        return new AlertSettingResponseDTO(
                exchangeType.getCode(),           // currencyCode
                exchangeType.getLabel(),          // currencyName
                exchangeType.getFlagImageUrl(),   // flagImageUrl
                false,                           // isTargetPriceEnabled
                false,                           // isDailyAlertEnabled
                null,                            // targetPrice
                null,                            // targetPricePushHow
                null                             // dailyAlertTime
        );
    }
    
    @Override
    public void saveTargetAlertSettings(String userEmail, String currencyCode, AlertTargetRequestDTO targetSettings) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + userEmail));
        
        // 지원하는 통화인지 확인
        try {
            ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "지원하지 않는 통화 코드입니다: " + currencyCode);
        }
        
        // 기존 설정 조회 또는 새로 생성
        AlertSetting alertSetting = alertSettingRepository
                .findByUserAndCurrencyCodeAndIsActiveTrue(user, currencyCode)
                .orElse(AlertSetting.builder()
                        .user(user)
                        .currencyCode(currencyCode)
                        .isActive(true)
                        .build());
        
        // 목표 환율 설정 업데이트
        if (targetSettings.isEnabled()) {
            if (targetSettings.getTargetPrice() == null || targetSettings.getCondition() == null) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "목표 환율과 조건을 모두 입력해주세요.");
            }
            alertSetting.updateTargetSettings(
                    true,
                    BigDecimal.valueOf(targetSettings.getTargetPrice()),
                    targetSettings.getCondition()
            );
        } else {
            alertSetting.updateTargetSettings(false, null, null);
        }
        
        alertSettingRepository.save(alertSetting);
        
        log.info("목표 환율 알림 설정 저장: 사용자={}, 통화={}, 활성화={}, 목표환율={}", 
                userEmail, currencyCode, targetSettings.isEnabled(), targetSettings.getTargetPrice());
    }
    
    @Override
    public void saveDailyAlertSettings(String userEmail, String currencyCode, AlertDailyRequestDTO dailySettings) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + userEmail));
        
        // 지원하는 통화인지 확인
        try {
            ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "지원하지 않는 통화 코드입니다: " + currencyCode);
        }
        
        // 기존 설정 조회 또는 새로 생성
        AlertSetting alertSetting = alertSettingRepository
                .findByUserAndCurrencyCodeAndIsActiveTrue(user, currencyCode)
                .orElse(AlertSetting.builder()
                        .user(user)
                        .currencyCode(currencyCode)
                        .isActive(true)
                        .build());
        
        // 일일 알림 설정 업데이트
        if (dailySettings.isEnabled()) {
            if (dailySettings.getAlertTime() == null) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "알림 시간을 입력해주세요.");
            }
            try {
                LocalTime alertTime = LocalTime.parse(dailySettings.getAlertTime());
                alertSetting.updateDailySettings(true, alertTime);
            } catch (Exception e) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "올바른 시간 형식이 아닙니다. (HH:mm)");
            }
        } else {
            alertSetting.updateDailySettings(false, null);
        }
        
        alertSettingRepository.save(alertSetting);
        
        log.info("일일 환율 알림 설정 저장: 사용자={}, 통화={}, 활성화={}, 알림시간={}", 
                userEmail, currencyCode, dailySettings.isEnabled(), dailySettings.getAlertTime());
    }
    
    @Override
    @Transactional(readOnly = true)
    public AlertTargetResponseDTO getTargetAlertSetting(Long userId, String currencyCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 ID: " + userId));
        
        // 지원하는 통화인지 확인
        ExchangeList.ExchangeType exchangeType;
        try {
            exchangeType = ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "지원하지 않는 통화 코드입니다: " + currencyCode);
        }
        
        Optional<AlertSetting> alertSetting = alertSettingRepository.findByUserAndCurrencyCodeAndIsActiveTrue(user, currencyCode);
        
        if (alertSetting.isPresent()) {
            AlertSetting setting = alertSetting.get();
            return AlertTargetResponseDTO.builder()
                    .currencyCode(currencyCode)
                    .currencyName(exchangeType.getLabel())
                    .flagImageUrl(exchangeType.getFlagImageUrl())
                    .isEnabled(setting.getTargetPricePush() != null ? setting.getTargetPricePush() : false)
                    .targetPrice(setting.getTargetPrice())
                    .condition(setting.getTargetPricePushHow())
                    .build();
        } else {
            // 설정이 없으면 기본값 반환
            return AlertTargetResponseDTO.builder()
                    .currencyCode(currencyCode)
                    .currencyName(exchangeType.getLabel())
                    .flagImageUrl(exchangeType.getFlagImageUrl())
                    .isEnabled(false)
                    .targetPrice(null)
                    .condition(null)
                    .build();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public AlertDailyResponseDTO getDailyAlertSetting(Long userId, String currencyCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 ID: " + userId));
        
        // 지원하는 통화인지 확인
        ExchangeList.ExchangeType exchangeType;
        try {
            exchangeType = ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "지원하지 않는 통화 코드입니다: " + currencyCode);
        }
        
        Optional<AlertSetting> alertSetting = alertSettingRepository.findByUserAndCurrencyCodeAndIsActiveTrue(user, currencyCode);
        
        if (alertSetting.isPresent()) {
            AlertSetting setting = alertSetting.get();
            return AlertDailyResponseDTO.builder()
                    .currencyCode(currencyCode)
                    .currencyName(exchangeType.getLabel())
                    .flagImageUrl(exchangeType.getFlagImageUrl())
                    .isEnabled(setting.getTodayExchangeRatePush() != null ? setting.getTodayExchangeRatePush() : false)
                    .alertTime(setting.getTodayExchangeRatePushTime() != null ? setting.getTodayExchangeRatePushTime().toString() : null)
                    .build();
        } else {
            // 설정이 없으면 기본값 반환
            return AlertDailyResponseDTO.builder()
                    .currencyCode(currencyCode)
                    .currencyName(exchangeType.getLabel())
                    .flagImageUrl(exchangeType.getFlagImageUrl())
                    .isEnabled(false)
                    .alertTime(null)
                    .build();
        }
    }
}