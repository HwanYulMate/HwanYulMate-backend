package com.swyp.api_server.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 은행 환전 정보 Entity
 * - 은행별 환율 스프레드, 우대율, 수수료 정보
 * - 관리자가 동적으로 수정 가능
 */
@Entity
@Table(name = "bank_exchange_info")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class BankExchangeInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "bank_name", nullable = false, length = 50, unique = true)
    private String bankName;
    
    @Column(name = "bank_code", nullable = false, length = 10, unique = true)
    private String bankCode;
    
    @Column(name = "spread_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal spreadRate;        // 스프레드율 (%)
    
    @Column(name = "preferential_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal preferentialRate;  // 우대율 (%)
    
    @Column(name = "fixed_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fixedFee;          // 고정 수수료
    
    @Column(name = "fee_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal feeRate;           // 수수료율 (%)
    
    @Column(name = "min_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minAmount;         // 최소 환전 금액
    
    @Column(name = "max_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxAmount;         // 최대 환전 금액
    
    @Column(name = "is_online_available", nullable = false)
    private Boolean isOnlineAvailable;    // 온라인 환전 가능 여부
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;      // 서비스 활성화 여부
    
    @Column(name = "description", length = 200)
    private String description;           // 부가 설명
    
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;     // 화면 표시 순서
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 은행 정보 업데이트
     */
    public void updateBankInfo(BigDecimal spreadRate, BigDecimal preferentialRate, 
                              BigDecimal fixedFee, BigDecimal feeRate,
                              BigDecimal minAmount, BigDecimal maxAmount,
                              Boolean isOnlineAvailable, String description) {
        this.spreadRate = spreadRate;
        this.preferentialRate = preferentialRate;
        this.fixedFee = fixedFee;
        this.feeRate = feeRate;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.isOnlineAvailable = isOnlineAvailable;
        this.description = description;
    }
    
    /**
     * 활성화 상태 변경
     */
    public void updateActiveStatus(Boolean isActive) {
        this.isActive = isActive;
    }
    
    /**
     * 표시 순서 변경
     */
    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}