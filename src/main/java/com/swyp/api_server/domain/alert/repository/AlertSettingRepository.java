package com.swyp.api_server.domain.alert.repository;

import com.swyp.api_server.entity.AlertSetting;
import com.swyp.api_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 알림 설정 Repository
 */
@Repository
public interface AlertSettingRepository extends JpaRepository<AlertSetting, Long> {
    
    /**
     * 사용자별 활성화된 알림 설정 조회
     */
    List<AlertSetting> findByUserAndIsActiveTrue(User user);
    
    /**
     * 사용자와 통화별 알림 설정 조회
     */
    Optional<AlertSetting> findByUserAndCurrencyCodeAndIsActiveTrue(User user, String currencyCode);
    
    /**
     * 목표 환율 알림이 활성화된 설정들 조회 (달성되지 않은 것만)
     */
    @Query("SELECT a FROM AlertSetting a WHERE a.isActive = true AND a.targetPricePush = true AND a.targetAchieved = false")
    List<AlertSetting> findActiveTargetPriceAlerts();
    
    /**
     * 목표 환율 알림이 활성화된 설정들 조회 (유효한 FCM 토큰 보유자만)
     */
    @Query("SELECT a FROM AlertSetting a JOIN a.user u WHERE a.isActive = true AND a.targetPricePush = true " +
           "AND a.targetAchieved = false AND u.fcmToken IS NOT NULL AND u.fcmToken != ''")
    List<AlertSetting> findActiveTargetPriceAlertsWithValidTokens();
    
    /**
     * 특정 시간대의 오늘의 환율 알림 설정 조회
     */
    @Query("SELECT a FROM AlertSetting a WHERE a.isActive = true AND a.todayExchangeRatePush = true " +
           "AND a.todayExchangeRatePushTime = :alertTime " +
           "AND (a.lastDailyAlertSent IS NULL OR DATE(a.lastDailyAlertSent) < CURRENT_DATE)")
    List<AlertSetting> findTodayExchangeRateAlertsForTime(@Param("alertTime") LocalTime alertTime);
    
    /**
     * 특정 시간대의 오늘의 환율 알림 설정 조회 (유효한 FCM 토큰 보유자만)
     */
    @Query("SELECT a FROM AlertSetting a JOIN a.user u WHERE a.isActive = true AND a.todayExchangeRatePush = true " +
           "AND a.todayExchangeRatePushTime = :alertTime " +
           "AND (a.lastDailyAlertSent IS NULL OR DATE(a.lastDailyAlertSent) < CURRENT_DATE) " +
           "AND u.fcmToken IS NOT NULL AND u.fcmToken != ''")
    List<AlertSetting> findTodayExchangeRateAlertsForTimeWithValidTokens(@Param("alertTime") LocalTime alertTime);
    
    /**
     * 사용자의 모든 알림 설정 조회 (비활성화 포함)
     */
    List<AlertSetting> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 통화별 활성화된 알림 설정 개수 조회
     */
    @Query("SELECT COUNT(a) FROM AlertSetting a WHERE a.currencyCode = :currencyCode AND a.isActive = true")
    Long countActiveByCurrencyCode(@Param("currencyCode") String currencyCode);
}