package com.swyp.api_server.domain.rate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.swyp.api_server.common.constants.Constants;
import com.swyp.api_server.common.http.CommonHttpClient;
import com.swyp.api_server.common.validator.CommonValidator;
import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.domain.rate.dto.response.ExchangeChartResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeRealtimeResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 환율 데이터 조회 서비스 구현체
 * - 한국 수출입은행 API 사용하여 공식 환율 정보 제공
 * - 캐싱 기능으로 API 호출 최적화 (하루 1000회 제한)
 * - 14개국 통화 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {
    
    @Value("${custom.koreaexim-api-key:SAMPLE_API_KEY}")
    private String apiKey;
    
    private final CommonHttpClient httpClient;
    private final CommonValidator validator;
    
    /**
     * 모든 통화의 실시간 환율 목록 조회
     * - 한국 수출입은행 공식 환율 데이터 사용
     * - 5분간 캐시하여 API 호출 최적화 (일 1000회 제한 고려)
     */
    @Override
    @Cacheable(value = Constants.Cache.EXCHANGE_RATES, cacheManager = "cacheManager")
    public List<ExchangeResponseDTO> getAllExchangeRates() {
        return getAllExchangeRatesWithoutCache();
    }
    
    /**
     * 캐시 없이 환율 데이터 조회 (다중 API + DB 백업)
     */
    public List<ExchangeResponseDTO> getAllExchangeRatesWithoutCache() {
        try {
            return getExchangeRatesFromKoreaExim();
        } catch (CustomException e) {
            // CustomException은 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("수출입은행 API 호출 실패", e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
                "환율 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 수출입은행 API로 환율 조회 (리팩토링된 메서드)
     */
    private List<ExchangeResponseDTO> getExchangeRatesFromKoreaExim() throws Exception {
        String searchDate = getCurrentOrPreviousDate();
        JsonNode responseData = fetchExchangeDataWithFallback(searchDate);
        return parseExchangeRatesFromResponse(responseData, searchDate);
    }
    
    /**
     * 현재 날짜 또는 이전 날짜 조회
     */
    private String getCurrentOrPreviousDate() throws Exception {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        try {
            JsonNode currentData = tryApiCall(currentDate);
            if (hasValidData(currentData)) {
                return currentDate;
            }
        } catch (Exception e) {
            log.warn("당일 환율 데이터 조회 실패: {}", e.getMessage());
        }
        
        log.info("당일 환율 데이터 없음, 전일 데이터 조회 시도");
        String previousDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return previousDate;
    }
    
    /**
     * 환율 데이터 조회 (폴백 포함)
     */
    private JsonNode fetchExchangeDataWithFallback(String searchDate) throws Exception {
        JsonNode responseData = tryApiCall(searchDate);
        
        if (!hasValidData(responseData)) {
            log.info("환율 데이터 없음, 전일 데이터 조회 시도");
            searchDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            responseData = tryApiCall(searchDate);
        }
        
        return responseData;
    }
    
    /**
     * 응답 데이터에서 환율 정보 파싱
     */
    private List<ExchangeResponseDTO> parseExchangeRatesFromResponse(JsonNode responseData, String searchDate) {
        List<ExchangeResponseDTO> exchangeRates = new ArrayList<>();
        
        for (ExchangeList.ExchangeType currency : ExchangeList.ExchangeType.values()) {
            ExchangeResponseDTO exchangeDto = parseExchangeRateForCurrency(responseData, currency, searchDate);
            if (exchangeDto != null) {
                exchangeRates.add(exchangeDto);
            }
        }
        
        return exchangeRates;
    }
    
    /**
     * 개별 통화 환율 파싱
     */
    private ExchangeResponseDTO parseExchangeRateForCurrency(JsonNode responseData, 
                                                           ExchangeList.ExchangeType currency, 
                                                           String searchDate) {
        String currencyCode = currency.getCode();
        String mappedCurrencyCode = mapToKoreaEximCurrencyCode(currencyCode);
        
        JsonNode currencyData = findCurrencyInResponse(responseData, mappedCurrencyCode);
        if (currencyData == null) {
            log.warn("수출입은행에서 환율 정보를 찾을 수 없습니다: {}", currencyCode);
            return null;
        }
        
        String dealBasRStr = currencyData.get("deal_bas_r").asText().replace(",", "");
        if (dealBasRStr.isEmpty() || dealBasRStr.equals("0")) {
            log.warn("수출입은행 환율 데이터가 없습니다: {}", currencyCode);
            return null;
        }
        
        BigDecimal exchangeRate = calculateActualExchangeRate(dealBasRStr, mappedCurrencyCode);
        
        return ExchangeResponseDTO.builder()
                .currencyCode(currencyCode)
                .currencyName(currency.getLabel())
                .flagImageUrl(currency.getFlagImageUrl())
                .exchangeRate(exchangeRate)
                .baseDate(searchDate)
                .build();
    }
    
    /**
     * 실제 환율 계산 (100 단위 통화 처리)
     */
    private BigDecimal calculateActualExchangeRate(String dealBasRStr, String mappedCurrencyCode) {
        BigDecimal exchangeRate = new BigDecimal(dealBasRStr);
        
        // JPY(100), IDR(100)만 100 단위이므로 1 단위로 환산
        // CNH는 100단위가 아니므로 그대로 사용
        if (mappedCurrencyCode.contains("(100)")) {
            exchangeRate = exchangeRate.divide(BigDecimal.valueOf(100), Constants.Exchange.DECIMAL_SCALE, RoundingMode.HALF_UP);
        }
        
        return exchangeRate;
    }
    
    /**
     * 응답 데이터 유효성 확인
     */
    private boolean hasValidData(JsonNode responseData) {
        return responseData != null && responseData.isArray() && responseData.size() > 0;
    }
    
    private JsonNode tryApiCall(String searchDate) throws Exception {
        Map<String, String> params = Map.of(
            "authkey", apiKey,
            "searchdate", searchDate,
            "data", Constants.Api.KOREA_EXIM_DATA_CODE
        );
        
        String url = httpClient.buildUrl(Constants.Api.KOREA_EXIM_BASE_URL, params);
        log.info("수출입은행 API 호출: {}", url);
        
        JsonNode responseData = httpClient.getJson(url);
        log.info("API 응답 길이: {}", responseData.toString().length());
        log.debug("수출입은행 API 응답 내용: {}", responseData);
        
        // 수출입은행 API 에러 응답 처리 (빈 배열은 여기서 처리하지 않음)
        handleKoreaEximApiResponse(responseData);
        
        return responseData;
    }
    
    /**
     * 특정 통화의 실시간 환율 및 등락률 조회
     */
    @Override
    @Cacheable(value = Constants.Cache.REALTIME_RATE, key = "#currencyCode")
    public ExchangeRealtimeResponseDTO getRealtimeExchangeRate(String currencyCode) {
        try {
            // 통화 코드 유효성 검증
            validator.validateCurrencyCode(currencyCode);
            
            String mappedCurrencyCode = mapToKoreaEximCurrencyCode(currencyCode);
            
            // 현재 환율 조회 (당일 → 전일 fallback)
            String searchDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            JsonNode currentData = tryApiCall(searchDate);
            int daysBack = 0;
            
            // 당일 데이터 없으면 전일 시도
            if (!currentData.isArray() || currentData.size() == 0) {
                log.info("당일 환율 데이터 없음, 전일 데이터로 실시간 환율 조회");
                searchDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                currentData = tryApiCall(searchDate);
                daysBack = 1;
                
                if (!currentData.isArray() || currentData.size() == 0) {
                    log.info("전일 환율 데이터도 없음, 전전일 데이터 시도");
                    searchDate = LocalDate.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    currentData = tryApiCall(searchDate);
                    daysBack = 2;
                    
                    if (!currentData.isArray() || currentData.size() == 0) {
                        throw new CustomException(ErrorCode.EXCHANGE_RATE_NOT_FOUND, "환율 데이터가 없습니다.");
                    }
                }
            }
            
            // 비교용 전일 환율 조회 (현재 데이터 기준 하루 전)
            String previousDate = LocalDate.now().minusDays(daysBack + 1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            JsonNode historicalData = tryApiCall(previousDate);
            
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
            BigDecimal currentRate = calculateActualExchangeRate(currentDealBasRStr, mappedCurrencyCode);
            
            // 전일 환율 파싱
            String previousDealBasRStr = previousCurrencyData.get("deal_bas_r").asText().replace(",", "");
            BigDecimal previousRate = calculateActualExchangeRate(previousDealBasRStr, mappedCurrencyCode);
            
            // 등락률 계산 (현재환율 - 전일환율) / 전일환율 * 100
            BigDecimal changeRate = currentRate.subtract(previousRate)
                    .divide(previousRate, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            // 등락폭 계산
            BigDecimal changeAmount = currentRate.subtract(previousRate);
            
            String currencyName = getCurrencyName(currencyCode);
            String flagImageUrl = getFlagImageUrl(currencyCode);
            
            return ExchangeRealtimeResponseDTO.builder()
                    .currencyCode(currencyCode)
                    .currencyName(currencyName)
                    .flagImageUrl(flagImageUrl)
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
    @Cacheable(value = Constants.Cache.HISTORICAL_RATE, key = "#currencyCode + '_' + #days")
    public List<ExchangeChartResponseDTO> getHistoricalExchangeRate(String currencyCode, int days) {
        try {
            validator.validateCurrencyCode(currencyCode);
            
            String mappedCurrencyCode = mapToKoreaEximCurrencyCode(currencyCode);
            List<ExchangeChartResponseDTO> chartData = new ArrayList<>();
            LocalDate endDate = LocalDate.now();
            
            // 최근 N일간의 환율 데이터 수집
            for (int i = days - 1; i >= 0; i--) {
                LocalDate targetDate = endDate.minusDays(i);
                String searchDate = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                
                try {
                    JsonNode jsonData = tryApiCall(searchDate);
                    
                    JsonNode currencyData = findCurrencyInResponse(jsonData, mappedCurrencyCode);
                    if (currencyData != null) {
                        String dealBasRStr = currencyData.get("deal_bas_r").asText().replace(",", "");
                        
                        if (!dealBasRStr.isEmpty() && !dealBasRStr.equals("0")) {
                            BigDecimal exchangeRate = calculateActualExchangeRate(dealBasRStr, mappedCurrencyCode);
                            
                            ExchangeChartResponseDTO dto = ExchangeChartResponseDTO.builder()
                                    .date(searchDate)
                                    .rate(exchangeRate)
                                    .timestamp(targetDate.atStartOfDay())
                                    .build();
                            
                            chartData.add(dto);
                        }
                    }
                    
                    // API 호출 간격 조절 (Rate Limiting 방지)
                    Thread.sleep(Constants.Api.API_RATE_LIMIT_DELAY_MS);
                    
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
     * 통화 코드에 해당하는 국기 이미지 URL 조회
     */
    private String getFlagImageUrl(String currencyCode) {
        try {
            return ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase()).getFlagImageUrl();
        } catch (IllegalArgumentException e) {
            return Constants.Image.DEFAULT_FLAG_IMAGE;
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
            case "JPY" -> Constants.Exchange.JPY_100_UNIT;   // 일본 엔 (100엔 단위)
            case "IDR" -> Constants.Exchange.IDR_100_UNIT;   // 인도네시아 루피아 (100 단위)
            case "CNY" -> Constants.Exchange.CNH_CODE;       // 중국 위안 (수출입은행에서는 CNH 사용)
            default -> standardCode;
        };
    }
    
    /**
     * 수출입은행 API 응답 에러 처리
     */
    private void handleKoreaEximApiResponse(JsonNode responseData) {
        // 배열 응답에서 개별 아이템의 result 체크 (에러 응답만 처리)
        if (responseData.isArray() && responseData.size() > 0) {
            JsonNode firstItem = responseData.get(0);
            if (firstItem.has("result")) {
                int result = firstItem.get("result").asInt();
                
                switch (result) {
                    case 1 -> log.debug("수출입은행 API 정상 응답");  // 성공
                    case 2 -> throw new CustomException(ErrorCode.INVALID_REQUEST, 
                        "DATA 코드 오류 (" + Constants.Api.KOREA_EXIM_DATA_CODE + " 확인)");
                    case 3 -> throw new CustomException(ErrorCode.INVALID_REQUEST, 
                        "인증키 오류. 발급받은 인증키를 확인해주세요.");
                    case 4 -> throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
                        "일일 호출 한도(" + Constants.Api.KOREA_EXIM_DAILY_LIMIT + "회)를 초과했습니다.");
                    default -> log.warn("알 수 없는 수출입은행 API 응답: {}", result);
                }
            }
        }
        // 빈 배열은 여기서 예외를 던지지 않음 - 호출하는 곳에서 fallback 처리
    }
    
}