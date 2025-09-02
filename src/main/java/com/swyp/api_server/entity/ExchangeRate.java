package com.swyp.api_server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates", 
       indexes = {
           @Index(name = "idx_currency_date", columnList = "currency_code, base_date"),
           @Index(name = "idx_base_date", columnList = "base_date"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ExchangeRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    
    @Column(name = "currency_name", nullable = false, length = 50)
    private String currencyName;
    
    @Column(name = "exchange_rate", nullable = false, precision = 15, scale = 4)
    private BigDecimal exchangeRate;
    
    @Column(name = "base_date", nullable = false, length = 8)
    private String baseDate;
    
    @Column(name = "source_api", nullable = false, length = 20)
    private String sourceApi;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public void updateRate(BigDecimal newRate) {
        this.exchangeRate = newRate;
    }
}