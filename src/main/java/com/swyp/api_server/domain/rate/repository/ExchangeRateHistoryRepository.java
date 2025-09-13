package com.swyp.api_server.domain.rate.repository;

import com.swyp.api_server.domain.rate.entity.ExchangeRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 환율 히스토리 리포지토리
 */
@Repository
public interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    /**
     * 특정 통화의 특정 날짜 환율 조회
     */
    Optional<ExchangeRateHistory> findByCurrencyCodeAndBaseDate(String currencyCode, LocalDate baseDate);

    /**
     * 특정 날짜의 모든 통화 환율 조회
     */
    List<ExchangeRateHistory> findByBaseDateOrderByCurrencyCode(LocalDate baseDate);

    /**
     * 특정 통화의 최근 N일 환율 히스토리 조회
     */
    @Query("SELECT h FROM ExchangeRateHistory h WHERE h.currencyCode = :currencyCode " +
           "ORDER BY h.baseDate DESC LIMIT :days")
    List<ExchangeRateHistory> findRecentHistoryByCurrency(@Param("currencyCode") String currencyCode, 
                                                          @Param("days") int days);

    /**
     * 특정 통화의 전일 환율 조회
     */
    @Query("SELECT h FROM ExchangeRateHistory h WHERE h.currencyCode = :currencyCode " +
           "AND h.baseDate < :currentDate ORDER BY h.baseDate DESC LIMIT 1")
    Optional<ExchangeRateHistory> findPreviousDayRate(@Param("currencyCode") String currencyCode, 
                                                      @Param("currentDate") LocalDate currentDate);

    /**
     * 전일의 모든 통화 환율 조회
     */
    @Query("SELECT h FROM ExchangeRateHistory h WHERE h.baseDate = :previousDate " +
           "ORDER BY h.currencyCode")
    List<ExchangeRateHistory> findAllByPreviousDate(@Param("previousDate") LocalDate previousDate);

    /**
     * 특정 날짜에 데이터가 있는지 확인
     */
    boolean existsByBaseDate(LocalDate baseDate);

    /**
     * 기간별 환율 데이터 조회 (차트용)
     */
    @Query("SELECT h FROM ExchangeRateHistory h WHERE h.currencyCode = :currencyCode " +
           "AND h.baseDate BETWEEN :startDate AND :endDate ORDER BY h.baseDate ASC")
    List<ExchangeRateHistory> findByPeriod(@Param("currencyCode") String currencyCode,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}