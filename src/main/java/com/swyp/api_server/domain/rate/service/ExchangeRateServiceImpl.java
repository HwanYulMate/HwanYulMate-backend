package com.swyp.api_server.domain.rate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.domain.rate.dto.response.ExchangeChartResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeRealtimeResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 환율 데이터 조회 서비스 구현체
 * - ExchangeRate-API (무료) 사용하여 실시간 환율 정보 제공
 * - 캐싱 기능으로 API 호출 최적화
 * - 16개국 통화 지원
 */
@Slf4j
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {
    
    private static final String BASE_URL = "https://api.exchangerate-api.com/v4";
    private static final String BASE_CURRENCY = "KRW"; // 한국 원화 기준
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public ExchangeRateServiceImpl() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 모든 통화의 실시간 환율 목록 조회
     * - 1분간 캐시하여 API 호출 최적화
     */
    @Override
    @Cacheable(value = "exchangeRates", cacheManager = "cacheManager")
    public List<ExchangeResponseDTO> getAllExchangeRates() {
        try {
            String url = BASE_URL + "/latest/" + BASE_CURRENCY;
            String response = makeApiCall(url);
            
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode ratesNode = jsonNode.get("rates");
            
            List<ExchangeResponseDTO> exchangeRates = new ArrayList<>();
            
            // 16개국 통화 순회하며 환율 정보 생성
            for (ExchangeList.ExchangeType currency : ExchangeList.ExchangeType.values()) {
                String currencyCode = currency.getCode();
                
                if (ratesNode.has(currencyCode)) {
                    BigDecimal rate = BigDecimal.valueOf(ratesNode.get(currencyCode).asDouble());
                    
                    // KRW 기준이므로 역수 계산 (1 외화 = ? 원)
                    BigDecimal krwRate = BigDecimal.ONE.divide(rate, 4, RoundingMode.HALF_UP);
                    
                    ExchangeResponseDTO dto = ExchangeResponseDTO.builder()
                            .currencyCode(currencyCode)
                            .currencyName(currency.getLabel())
                            .exchangeRate(krwRate)
                            .baseDate(LocalDate.now().toString())
                            .build();
                    
                    exchangeRates.add(dto);
                } else {
                    log.warn("환율 정보를 찾을 수 없습니다: {}", currencyCode);
                }
            }
            
            log.info("환율 정보 조회 완료: {}개 통화", exchangeRates.size());
            return exchangeRates;
            
        } catch (Exception e) {
            log.error("전체 환율 조회 중 오류 발생", e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, "전체 환율 조회 실패", e);
        }
    }
    
    /**
     * 특정 통화의 실시간 환율 및 등락률 조회
     */
    @Override
    @Cacheable(value = "realtimeRate", key = "#currencyCode")
    public ExchangeRealtimeResponseDTO getRealtimeExchangeRate(String currencyCode) {
        try {
            // 통화 코드 유효성 검증
            validateCurrencyCode(currencyCode);
            
            // 현재 환율 조회
            String currentUrl = BASE_URL + "/latest/" + BASE_CURRENCY;
            String currentResponse = makeApiCall(currentUrl);
            JsonNode currentData = objectMapper.readTree(currentResponse);
            
            // 하루 전 환율 조회 (등락률 계산용)
            String yesterday = LocalDate.now().minusDays(1).toString();
            String historicalUrl = BASE_URL + "/history/" + BASE_CURRENCY + "/" + yesterday;
            String historicalResponse = makeApiCall(historicalUrl);
            JsonNode historicalData = objectMapper.readTree(historicalResponse);
            
            // 현재 환율
            BigDecimal currentRate = BigDecimal.valueOf(
                currentData.get("rates").get(currencyCode).asDouble());
            BigDecimal currentKrwRate = BigDecimal.ONE.divide(currentRate, 4, RoundingMode.HALF_UP);
            
            // 전일 환율 (등락률 계산용)
            BigDecimal previousRate = BigDecimal.valueOf(
                historicalData.get("rates").get(currencyCode).asDouble());
            BigDecimal previousKrwRate = BigDecimal.ONE.divide(previousRate, 4, RoundingMode.HALF_UP);
            
            // 등락률 계산 (현재환율 - 전일환율) / 전일환율 * 100
            BigDecimal changeRate = currentKrwRate.subtract(previousKrwRate)
                    .divide(previousKrwRate, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            // 등락폭 계산
            BigDecimal changeAmount = currentKrwRate.subtract(previousKrwRate);
            
            String currencyName = getCurrencyName(currencyCode);
            
            return ExchangeRealtimeResponseDTO.builder()
                    .currencyCode(currencyCode)
                    .currencyName(currencyName)
                    .currentRate(currentKrwRate)
                    .previousRate(previousKrwRate)
                    .changeAmount(changeAmount)
                    .changeRate(changeRate)
                    .updateTime(LocalDateTime.now())
                    .build();
                    
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("실시간 환율 조회 중 오류 발생: {}", currencyCode, e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
                "실시간 환율 조회 실패: " + currencyCode, e);
        }
    }
    
    /**
     * 특정 통화의 차트 데이터 조회 (최근 30일)
     */
    @Override
    public List<ExchangeChartResponseDTO> getExchangeChartData(String currencyCode) {
        return getHistoricalExchangeRate(currencyCode, 30);
    }
    
    /**
     * 특정 통화의 과거 환율 조회
     */
    @Override
    @Cacheable(value = "historicalRate", key = "#currencyCode + '_' + #days")
    public List<ExchangeChartResponseDTO> getHistoricalExchangeRate(String currencyCode, int days) {
        try {
            validateCurrencyCode(currencyCode);
            
            List<ExchangeChartResponseDTO> chartData = new ArrayList<>();
            LocalDate endDate = LocalDate.now();
            
            // 최근 N일간의 환율 데이터 수집
            for (int i = days - 1; i >= 0; i--) {
                LocalDate targetDate = endDate.minusDays(i);
                String dateStr = targetDate.toString();
                
                try {
                    String url = BASE_URL + "/history/" + BASE_CURRENCY + "/" + dateStr;
                    String response = makeApiCall(url);
                    JsonNode jsonData = objectMapper.readTree(response);
                    
                    if (jsonData.has("rates") && jsonData.get("rates").has(currencyCode)) {
                        BigDecimal rate = BigDecimal.valueOf(
                            jsonData.get("rates").get(currencyCode).asDouble());
                        BigDecimal krwRate = BigDecimal.ONE.divide(rate, 4, RoundingMode.HALF_UP);
                        
                        ExchangeChartResponseDTO dto = ExchangeChartResponseDTO.builder()
                                .date(dateStr)
                                .rate(krwRate)
                                .timestamp(targetDate.atStartOfDay())
                                .build();
                        
                        chartData.add(dto);
                    }
                    
                    // API 호출 간격 조절 (Rate Limiting 방지)
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.warn("{}일자 환율 데이터 조회 실패: {}", dateStr, e.getMessage());
                    // 개별 날짜 실패는 전체 실패로 이어지지 않도록 계속 진행
                }
            }
            
            log.info("과거 환율 데이터 조회 완료: {} ({} days)", currencyCode, chartData.size());
            return chartData;
            
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("과거 환율 조회 중 오류 발생: {}, {} days", currencyCode, days, e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
                "과거 환율 조회 실패: " + currencyCode, e);
        }
    }
    
    /**
     * HTTP API 호출 공통 메서드
     */
    private String makeApiCall(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API 호출 실패: HTTP " + response.code());
            }
            
            String responseBody = response.body().string();
            log.debug("API 응답 수신: {}", url);
            return responseBody;
        }
    }
    
    /**
     * 통화 코드 유효성 검증
     */
    private void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_CURRENCY_CODE, "통화 코드가 비어있습니다.");
        }
        
        try {
            ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CURRENCY_CODE, 
                "지원하지 않는 통화 코드입니다: " + currencyCode);
        }
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
}