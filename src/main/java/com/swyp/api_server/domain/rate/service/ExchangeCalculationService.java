package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.dto.request.ExchangeCalculationRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResultResponseDTO;

import java.util.List;

/**
 * 환전 계산 서비스 인터페이스
 * - 은행별 환전 예상 금액 계산
 * - 우대율 및 수수료 적용
 */
public interface ExchangeCalculationService {
    
    /**
     * 여러 은행의 환전 예상 금액 비교
     * @param request 환전 계산 요청 정보
     * @return 은행별 환전 결과 리스트
     */
    List<ExchangeResultResponseDTO> calculateExchangeRates(ExchangeCalculationRequestDTO request);
    
    /**
     * 특정 은행의 환전 예상 금액 계산
     * @param request 환전 계산 요청 정보
     * @param bankName 은행명
     * @return 해당 은행의 환전 결과
     */
    ExchangeResultResponseDTO calculateExchangeRate(ExchangeCalculationRequestDTO request, String bankName);
}