package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeRealtimeResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeChartResponseDTO;

import java.util.List;

/**
 * 환율 데이터 조회 서비스 인터페이스
 * - 외부 환율 API와의 연동 담당
 * - 실시간 환율, 차트 데이터, 과거 환율 정보 제공
 */
public interface ExchangeRateService {
    
    /**
     * 모든 통화의 실시간 환율 목록 조회
     * @return 12개국 환율 정보 리스트
     */
    List<ExchangeResponseDTO> getAllExchangeRates();
    
    /**
     * 특정 통화의 환율 조회 (개별 캐시)
     * @param currencyCode 통화 코드 (USD, EUR, CNY 등)
     * @return 특정 통화의 환율 정보
     */
    ExchangeResponseDTO getSingleExchangeRate(String currencyCode);
    
    /**
     * 특정 통화의 실시간 환율 및 등락률 조회
     * @param currencyCode 통화 코드 (USD, EUR, CNY 등)
     * @return 실시간 환율과 등락률 정보
     */
    ExchangeRealtimeResponseDTO getRealtimeExchangeRate(String currencyCode);
    
    /**
     * 특정 통화의 차트 데이터 조회 (최근 30일)
     * @param currencyCode 통화 코드
     * @return 차트용 시계열 데이터
     */
    List<ExchangeChartResponseDTO> getExchangeChartData(String currencyCode);
    
    /**
     * 특정 통화의 과거 환율 조회
     * @param currencyCode 통화 코드
     * @param days 조회할 일수 (7일, 30일 등)
     * @return 과거 환율 데이터
     */
    List<ExchangeChartResponseDTO> getHistoricalExchangeRate(String currencyCode, int days);
    
    /**
     * 환율 데이터 새로고침 (스케줄러용)
     */
    void refreshExchangeRates();
}