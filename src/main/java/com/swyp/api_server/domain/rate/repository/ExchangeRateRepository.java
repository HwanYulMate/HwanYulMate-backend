package com.swyp.api_server.domain.rate.repository;

import com.swyp.api_server.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    
    /**
     * 특정 날짜의 모든 환율 조회
     */
    List<ExchangeRate> findByBaseDateOrderByCurrencyCodeAsc(String baseDate);
    
    /**
     * 특정 통화의 최신 환율 조회
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.currencyCode = :currencyCode ORDER BY er.baseDate DESC, er.createdAt DESC LIMIT 1")
    Optional<ExchangeRate> findLatestByCurrencyCode(@Param("currencyCode") String currencyCode);
    
    /**
     * 특정 날짜와 통화의 환율 조회
     */
    Optional<ExchangeRate> findByCurrencyCodeAndBaseDate(String currencyCode, String baseDate);
    
    /**
     * 최신 날짜의 모든 환율 조회
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.baseDate = (SELECT MAX(er2.baseDate) FROM ExchangeRate er2) ORDER BY er.currencyCode ASC")
    List<ExchangeRate> findAllLatestRates();
    
    /**
     * 특정 통화의 과거 N일간 환율 조회
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.currencyCode = :currencyCode ORDER BY er.baseDate DESC LIMIT :days")
    List<ExchangeRate> findHistoricalRates(@Param("currencyCode") String currencyCode, @Param("days") int days);
    
    /**
     * 특정 날짜 이전 데이터 삭제 (데이터 정리용)
     */
    void deleteByBaseDateBefore(String baseDate);
}