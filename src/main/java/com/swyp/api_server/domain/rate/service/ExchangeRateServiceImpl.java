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
 * - 한국 수출입은행 API 사용하여 공식 환율 정보 제공
 * - 캐싱 기능으로 API 호출 최적화 (하루 1000회 제한)
 * - 16개국 통화 지원
 */
@Slf4j
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {
    
    private static final String BASE_URL = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON";
    
    @Value("${custom.koreaexim-api-key:SAMPLE_API_KEY}")
    private String apiKey;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public ExchangeRateServiceImpl() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 모든 통화의 실시간 환율 목록 조회
     * - 한국 수출입은행 공식 환율 데이터 사용
     * - 5분간 캐시하여 API 호출 최적화 (일 1000회 제한 고려)
     */
    @Override
    @Cacheable(value = "exchangeRates", cacheManager = "cacheManager")
    public List<ExchangeResponseDTO> getAllExchangeRates() {
        try {
            // 수출입은행 API: 당일 환율 조회
            String searchDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String url = BASE_URL + "?authkey=" + apiKey + "&searchdate=" + searchDate + "&data=AP01";
            
            String response = makeApiCall(url);
            JsonNode responseData = objectMapper.readTree(response);
            
            // 수출입은행 API 에러 응답 처리
            handleKoreaEximApiResponse(responseData);
            
            List<ExchangeResponseDTO> exchangeRates = new ArrayList<>();
            
            // 16개국 통화 순회하며 환율 정보 매핑
            for (ExchangeList.ExchangeType currency : ExchangeList.ExchangeType.values()) {
                String currencyCode = currency.getCode();
                
                // 수출입은행 통화 코드 매핑 (JPY(100), IDR(100) 등)
                String mappedCurrencyCode = mapToKoreaEximCurrencyCode(currencyCode);
                
                // 수출입은행 응답에서 해당 통화 찾기  
                JsonNode currencyData = findCurrencyInResponse(responseData, mappedCurrencyCode);
                
                if (currencyData != null) {
                    // 매매기준율 사용 (deal_bas_r)
                    String dealBasRStr = currencyData.get("deal_bas_r").asText().replace(",", "");
                    
                    if (!dealBasRStr.isEmpty() && !dealBasRStr.equals("0")) {
                        BigDecimal exchangeRate = new BigDecimal(dealBasRStr);
                        
                        // JPY(100), IDR(100) 등은 100 단위이므로 1 단위로 환산
                        if (mappedCurrencyCode.contains("(100)")) {
                            exchangeRate = exchangeRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        }
                        
                        ExchangeResponseDTO dto = ExchangeResponseDTO.builder()
                                .currencyCode(currencyCode)
                                .currencyName(currency.getLabel())
                                .exchangeRate(exchangeRate)
                                .baseDate(searchDate)
                                .build();
                        
                        exchangeRates.add(dto);
                    } else {
                        log.warn("수출입은행 환율 데이터가 없습니다: {}", currencyCode);
                    }
                } else {
                    log.warn("수출입은행에서 환율 정보를 찾을 수 없습니다: {}", currencyCode);
                }
            }
            
            log.info("수출입은행 환율 정보 조회 완료: {}개 통화", exchangeRates.size());
            return exchangeRates;
            
        } catch (Exception e) {
            log.error("수출입은행 환율 조회 중 오류 발생", e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, "환율 조회 실패", e);
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
            
            String mappedCurrencyCode = mapToKoreaEximCurrencyCode(currencyCode);
            
            // 현재 환율 조회 (수출입은행 API)
            String searchDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String currentUrl = BASE_URL + "?authkey=" + apiKey + "&searchdate=" + searchDate + "&data=AP01";
            String currentResponse = makeApiCall(currentUrl);
            JsonNode currentData = objectMapper.readTree(currentResponse);
            
            handleKoreaEximApiResponse(currentData);
            
            // 전일 환율 조회 (등락률 계산용)
            String previousDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String historicalUrl = BASE_URL + "?authkey=" + apiKey + "&searchdate=" + previousDate + "&data=AP01";
            String historicalResponse = makeApiCall(historicalUrl);
            JsonNode historicalData = objectMapper.readTree(historicalResponse);
            
            handleKoreaEximApiResponse(historicalData);
            
            // 현재 환율 데이터 찾기
            JsonNode currentCurrencyData = findCurrencyInResponse(currentData, mappedCurrencyCode);
            if (currentCurrencyData == null) {
                throw new CustomException(ErrorCode.EXCHANGE_RATE_NOT_FOUND, "현재 환율 데이터를 찾을 수 없습니다: " + currencyCode);
            }
            
            // 전일 환율 데이터 찾기
            JsonNode previousCurrencyData = findCurrencyInResponse(historicalData, mappedCurrencyCode);
            if (previousCurrencyData == null) {
                throw new CustomException(ErrorCode.EXCHANGE_RATE_NOT_FOUND, "전일 환율 데이터를 찾을 수 없습니다: " + currencyCode);
            }
            
            // 현재 환율 파싱
            String currentDealBasRStr = currentCurrencyData.get("deal_bas_r").asText().replace(",", "");
            BigDecimal currentRate = new BigDecimal(currentDealBasRStr);
            if (mappedCurrencyCode.contains("(100)")) {
                currentRate = currentRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            }
            
            // 전일 환율 파싱
            String previousDealBasRStr = previousCurrencyData.get("deal_bas_r").asText().replace(",", "");
            BigDecimal previousRate = new BigDecimal(previousDealBasRStr);
            if (mappedCurrencyCode.contains("(100)")) {
                previousRate = previousRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            }
            
            // 등락률 계산 (현재환율 - 전일환율) / 전일환율 * 100
            BigDecimal changeRate = currentRate.subtract(previousRate)
                    .divide(previousRate, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            // 등락폭 계산
            BigDecimal changeAmount = currentRate.subtract(previousRate);
            
            String currencyName = getCurrencyName(currencyCode);
            
            return ExchangeRealtimeResponseDTO.builder()
                    .currencyCode(currencyCode)
                    .currencyName(currencyName)
                    .currentRate(currentRate)
                    .previousRate(previousRate)
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
            
            String mappedCurrencyCode = mapToKoreaEximCurrencyCode(currencyCode);
            List<ExchangeChartResponseDTO> chartData = new ArrayList<>();
            LocalDate endDate = LocalDate.now();
            
            // 최근 N일간의 환율 데이터 수집
            for (int i = days - 1; i >= 0; i--) {
                LocalDate targetDate = endDate.minusDays(i);
                String searchDate = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                
                try {
                    String url = BASE_URL + "?authkey=" + apiKey + "&searchdate=" + searchDate + "&data=AP01";
                    String response = makeApiCall(url);
                    JsonNode jsonData = objectMapper.readTree(response);
                    
                    handleKoreaEximApiResponse(jsonData);
                    
                    JsonNode currencyData = findCurrencyInResponse(jsonData, mappedCurrencyCode);
                    if (currencyData != null) {
                        String dealBasRStr = currencyData.get("deal_bas_r").asText().replace(",", "");
                        
                        if (!dealBasRStr.isEmpty() && !dealBasRStr.equals("0")) {
                            BigDecimal exchangeRate = new BigDecimal(dealBasRStr);
                            
                            if (mappedCurrencyCode.contains("(100)")) {
                                exchangeRate = exchangeRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                            }
                            
                            ExchangeChartResponseDTO dto = ExchangeChartResponseDTO.builder()
                                    .date(searchDate)
                                    .rate(exchangeRate)
                                    .timestamp(targetDate.atStartOfDay())
                                    .build();
                            
                            chartData.add(dto);
                        }
                    }
                    
                    // API 호출 간격 조절 (Rate Limiting 방지)
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.warn("{}일자 환율 데이터 조회 실패: {}", searchDate, e.getMessage());
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
    
    /**
     * 수출입은행 API 응답에서 특정 통화 데이터 찾기
     */
    private JsonNode findCurrencyInResponse(JsonNode responseArray, String targetCurrencyCode) {
        if (!responseArray.isArray()) {
            return null;
        }
        
        for (JsonNode item : responseArray) {
            String curUnit = item.get("cur_unit").asText();
            
            // 수출입은행에서는 통화 단위가 다를 수 있음 (예: USD, JPY(100) 등)
            String cleanCurUnit = curUnit.replaceAll("\\(.*\\)", "").trim();
            
            if (cleanCurUnit.equalsIgnoreCase(targetCurrencyCode)) {
                return item;
            }
        }
        
        return null;
    }
    
    /**
     * 수출입은행 통화 코드 매핑
     * (수출입은행 응답 예시에 따른 실제 매핑)
     */
    private String mapToKoreaEximCurrencyCode(String standardCode) {
        return switch (standardCode.toUpperCase()) {
            case "JPY" -> "JPY(100)";   // 일본 엔 (100엔 단위)
            case "IDR" -> "IDR(100)";   // 인도네시아 루피아 (100 단위)
            case "CNY" -> "CNH";        // 중국 위안 (수출입은행에서는 CNH 사용)
            default -> standardCode;
        };
    }
    
    /**
     * 수출입은행 API 응답 에러 처리
     */
    private void handleKoreaEximApiResponse(JsonNode responseData) {
        // 정상 응답이지만 배열이 비어있는 경우
        if (responseData.isArray() && responseData.size() == 0) {
            throw new CustomException(ErrorCode.EXCHANGE_RATE_NOT_FOUND, "환율 데이터가 없습니다.");
        }
        
        // 배열 응답에서 개별 아이템의 result 체크
        if (responseData.isArray() && responseData.size() > 0) {
            JsonNode firstItem = responseData.get(0);
            if (firstItem.has("result")) {
                int result = firstItem.get("result").asInt();
                
                switch (result) {
                    case 1 -> log.debug("수출입은행 API 정상 응답");  // 성공
                    case 2 -> throw new CustomException(ErrorCode.INVALID_REQUEST, "DATA 코드 오류 (AP01 확인)");
                    case 3 -> throw new CustomException(ErrorCode.INVALID_REQUEST, "인증키 오류. 발급받은 인증키를 확인해주세요.");
                    case 4 -> throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, "일일 호출 한도(1000회)를 초과했습니다.");
                    default -> log.warn("알 수 없는 수출입은행 API 응답: {}", result);
                }
            }
        }
    }
}