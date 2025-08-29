package com.swyp.api_server.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 환율 알림 설정 Entity
 * - 사용자별 목표 환율 달성 알림
 * - 오늘의 환율 안내 알림 설정
 */
@Entity
@Table(name = "alert_settings")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AlertSetting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;        // 통화 코드 (USD, EUR 등)
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;    // 알림 활성화 여부
    
    // 목표 환율 달성 알림 설정
    @Column(name = "target_price_push", nullable = false)
    @Builder.Default
    private Boolean targetPricePush = false;    // 목표 환율 알림 사용 여부
    
    @Column(name = "target_price", precision = 10, scale = 2)
    private BigDecimal targetPrice;             // 목표 환율
    
    @Column(name = "target_price_push_how", length = 10)
    private String targetPricePushHow;          // 알림 수단 (PUSH, KAKAO)
    
    @Column(name = "target_achieved", nullable = false)
    @Builder.Default
    private Boolean targetAchieved = false;     // 목표 달성 여부
    
    // 오늘의 환율 안내 알림 설정
    @Column(name = "today_exchange_rate_push", nullable = false)
    @Builder.Default
    private Boolean todayExchangeRatePush = false;  // 오늘의 환율 알림 사용 여부
    
    @Column(name = "today_exchange_rate_push_time")
    private LocalTime todayExchangeRatePushTime;    // 알림 발송 시간
    
    @Column(name = "last_daily_alert_sent")
    private LocalDateTime lastDailyAlertSent;       // 마지막 일일 알림 발송 시간
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 알림 설정 업데이트
     */
    public void updateAlertSettings(Boolean targetPricePush, BigDecimal targetPrice,
                                  String targetPricePushHow, Boolean todayExchangeRatePush,
                                  LocalTime todayExchangeRatePushTime) {
        this.targetPricePush = targetPricePush;
        this.targetPrice = targetPrice;
        this.targetPricePushHow = targetPricePushHow;
        this.todayExchangeRatePush = todayExchangeRatePush;
        this.todayExchangeRatePushTime = todayExchangeRatePushTime;
        
        // 목표 환율이 변경되면 달성 상태 초기화
        this.targetAchieved = false;
    }
    
    /**
     * 목표 환율 달성 처리
     */
    public void markTargetAchieved() {
        this.targetAchieved = true;
    }
    
    /**
     * 일일 알림 발송 시간 업데이트
     */
    public void updateLastDailyAlertSent() {
        this.lastDailyAlertSent = LocalDateTime.now();
    }
    
    /**
     * 알림 활성화/비활성화
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }
}