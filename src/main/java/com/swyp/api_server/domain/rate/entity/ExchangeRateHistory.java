package com.swyp.api_server.domain.rate.entity;

import com.swyp.api_server.entity.ExchangeRate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 환율 히스토리 엔티티
 * - 매일 환율 데이터 저장용
 * - 전일 대비 변동률 계산 기준 데이터
 */
@Entity
@Table(name = "exchange_rate_history",
       indexes = {
           @Index(name = "idx_currency_date", columnList = "currency_code,base_date"),
           @Index(name = "idx_base_date", columnList = "base_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ExchangeRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 통화 코드 (USD, EUR, JPY 등)
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /**
     * 통화명 (미국 달러, 유럽 유로 등)
     */
    @Column(name = "currency_name", nullable = false, length = 50)
    private String currencyName;

    /**
     * 환율 (1 단위당 원화 가격)
     */
    @Column(name = "exchange_rate", nullable = false, precision = 15, scale = 4)
    private BigDecimal exchangeRate;

    /**
     * 기준일 (환율 데이터 날짜)
     */
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    /**
     * 생성 일시
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ExchangeRateHistory(String currencyCode, String currencyName, BigDecimal exchangeRate, LocalDate baseDate) {
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.exchangeRate = exchangeRate;
        this.baseDate = baseDate;
    }

    /**
     * ExchangeRate 엔티티로부터 히스토리 생성
     * ExchangeRate의 String baseDate(yyyyMMdd)를 LocalDate로 변환
     */
    public static ExchangeRateHistory from(ExchangeRate exchangeRate, LocalDate baseDate) {
        // baseDate 매개변수가 null인 경우 ExchangeRate의 baseDate를 파싱
        LocalDate actualBaseDate = baseDate;
        if (actualBaseDate == null && exchangeRate.getBaseDate() != null) {
            try {
                actualBaseDate = LocalDate.parse(exchangeRate.getBaseDate(), 
                    DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e) {
                // 파싱 실패 시 현재 날짜 사용
                actualBaseDate = LocalDate.now();
            }
        }
        
        return ExchangeRateHistory.builder()
                .currencyCode(exchangeRate.getCurrencyCode())
                .currencyName(exchangeRate.getCurrencyName())
                .exchangeRate(exchangeRate.getExchangeRate())
                .baseDate(actualBaseDate)
                .build();
    }
    
    /**
     * ExchangeRate 엔티티로부터 히스토리 생성 (baseDate 자동 변환)
     */
    public static ExchangeRateHistory from(ExchangeRate exchangeRate) {
        return from(exchangeRate, null);
    }
}