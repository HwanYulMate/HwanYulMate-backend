package com.swyp.api_server.domain.rate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.swyp.api_server.common.constants.Constants;
import com.swyp.api_server.common.http.CommonHttpClient;
import com.swyp.api_server.common.validator.CommonValidator;
import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.domain.rate.dto.response.ExchangeChartResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeRealtimeResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import com.swyp.api_server.domain.rate.entity.ExchangeRateHistory;
import com.swyp.api_server.domain.rate.repository.ExchangeRateHistoryRepository;
import com.swyp.api_server.domain.rate.repository.ExchangeRateRepository;
import com.swyp.api_server.entity.ExchangeRate;
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
import java.util.Optional;

/**
 * 환율 데이터 조회 서비스 구현체
 * - 한국 수출입은행 API 사용하여 공식 환율 정보 제공
 * - 캐싱 기능으로 API 호출 최적화 (하루 1000회 제한)
 * - 12개국 통화 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {
    
    @Value("${custom.koreaexim-api-key:SAMPLE_API_KEY}")
    private String apiKey;
    
    private final CommonHttpClient httpClient;
    private final CommonValidator validator;
    private final ExchangeRateHistoryRepository historyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    
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
     * 캐시 없이 환율 데이터 조회 - DB 전용 (API 호출 없음)
     * 스케줄러가 수집한 DB 데이터만 조회
     */
    public List<ExchangeResponseDTO> getAllExchangeRatesWithoutCache() {
        try {
            log.info("DB에서 캐시 없이 전체 환율 조회");
            return getAllExchangeRatesFromDatabase();
        } catch (Exception e) {
            log.error("DB에서 환율 데이터 조회 실패", e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
                "환율 데이터베이스 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 수출입은행 API로 환율 조회 (스케줄러 전용)
     * 클라이언트 요청에서는 사용 금지, 오직 스케줄러만 사용
     */
    public List<ExchangeResponseDTO> getExchangeRatesFromKoreaEximForScheduler() throws Exception {
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
        
        // 현재 지원하는 통화는 모두 1 단위이므로 그대로 사용
        
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
        boolean isRateLimitExceeded = handleKoreaEximApiResponse(responseData);
        if (isRateLimitExceeded) {
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_LIMIT_EXCEEDED, 
                "API 호출 한도 초과 - DB 데이터 사용 필요");
        }
        
        return responseData;
    }
    
    /**
     * 특정 통화의 실시간 환율 및 등락률 조회 - DB 전용 (API 호출 없음)
     * 스케줄러가 정해진 시간에 수집한 DB 데이터만 사용
     */
    @Override
    @Cacheable(value = Constants.Cache.REALTIME_RATE, key = "#currencyCode")
    public ExchangeRealtimeResponseDTO getRealtimeExchangeRate(String currencyCode) {
        try {
            // 통화 코드 유효성 검증
            validator.validateCurrencyCode(currencyCode);
            
            log.info("DB에서 실시간 환율 조회: {}", currencyCode);
            
            // 1. DB에서 현재 환율 조회 (exchange_rates 테이블)
            ExchangeRate currentExchangeRate = getLatestExchangeRateFromDB(currencyCode);
            if (currentExchangeRate == null) {
                log.warn("DB에서 현재 환율 데이터를 찾을 수 없습니다: {}", currencyCode);
                throw new CustomException(ErrorCode.EXCHANGE_RATE_NOT_FOUND, 
                    "환율 데이터를 찾을 수 없습니다. 스케줄러 동작을 확인해주세요: " + currencyCode);
            }
            
            // 2. DB에서 전일 환율 조회 (exchange_rate_history 테이블)
            LocalDate currentDate = LocalDate.now();
            ExchangeRateHistory previousDayHistory = getPreviousDayRateFromDB(currencyCode, currentDate);
            
            if (previousDayHistory == null) {
                log.warn("DB에서 전일 환율 데이터를 찾을 수 없습니다: {}", currencyCode);
                // 전일 데이터가 없어도 현재 환율은 반환 (변동률은 0으로)
                return ExchangeRealtimeResponseDTO.builder()
                        .currencyCode(currencyCode)
                        .currencyName(getCurrencyName(currencyCode))
                        .flagImageUrl(getFlagImageUrl(currencyCode))
                        .currentRate(currentExchangeRate.getExchangeRate())
                        .previousRate(currentExchangeRate.getExchangeRate())
                        .changeAmount(BigDecimal.ZERO)
                        .changeRate(BigDecimal.ZERO)
                        .updateTime(LocalDateTime.now())
                        .build();
            }
            
            // 3. 변동률 계산
            BigDecimal currentRate = currentExchangeRate.getExchangeRate();
            BigDecimal previousRate = previousDayHistory.getExchangeRate();
            
            // 등락률 계산 (현재환율 - 전일환율) / 전일환율 * 100
            BigDecimal changeRate = currentRate.subtract(previousRate)
                    .divide(previousRate, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            // 등락폭 계산
            BigDecimal changeAmount = currentRate.subtract(previousRate);
            
            log.info("DB 실시간 환율 조회 완료: {} (현재: {}, 전일: {}, 변동률: {}%)", 
                    currencyCode, currentRate, previousRate, changeRate);
            
            return ExchangeRealtimeResponseDTO.builder()
                    .currencyCode(currencyCode)
                    .currencyName(getCurrencyName(currencyCode))
                    .flagImageUrl(getFlagImageUrl(currencyCode))
                    .currentRate(currentRate)
                    .previousRate(previousRate)
                    .changeAmount(changeAmount)
                    .changeRate(changeRate)
                    .updateTime(LocalDateTime.now())
                    .build();
                    
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("DB 실시간 환율 조회 중 오류 발생: {}", currencyCode, e);
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
     * 특정 통화의 과거 환율 조회 - DB 전용 (API 호출 없음)
     * 스케줄러가 정해진 시간에 API 호출 → DB 저장하므로 클라이언트는 DB에서만 조회
     */
    @Override
    public List<ExchangeChartResponseDTO> getHistoricalExchangeRate(String currencyCode, int days) {
        try {
            // 캐시에서 조회 시도
            return getHistoricalExchangeRateFromCache(currencyCode, days);
        } catch (Exception e) {
            log.warn("캐시 조회 실패, DB에서 직접 조회: {}, {} days, error: {}", currencyCode, days, e.getMessage());
            // 캐시 실패 시 DB에서 직접 조회
            return getHistoricalDataFromDatabaseSafely(currencyCode, days);
        }
    }
    
    /**
     * 캐시를 통한 환율 히스토리 조회
     */
    @Cacheable(value = Constants.Cache.HISTORICAL_RATE, key = "#currencyCode + '_' + #days")
    public List<ExchangeChartResponseDTO> getHistoricalExchangeRateFromCache(String currencyCode, int days) {
        validator.validateCurrencyCode(currencyCode);
        
        log.info("캐시를 통한 환율 히스토리 조회: {} ({} days)", currencyCode, days);
        return getHistoricalDataFromDatabaseSafely(currencyCode, days);
    }
    
    /**
     * 안전한 DB 조회 (예외 처리 강화)
     */
    private List<ExchangeChartResponseDTO> getHistoricalDataFromDatabaseSafely(String currencyCode, int days) {
        try {
            validator.validateCurrencyCode(currencyCode);
            
            log.info("DB에서 환율 히스토리 조회: {} ({} days)", currencyCode, days);
            List<ExchangeChartResponseDTO> dbData = getHistoricalDataFromDatabase(currencyCode, days);
            
            if (!dbData.isEmpty()) {
                log.info("DB 환율 데이터 반환: {} ({} entries)", currencyCode, dbData.size());
            } else {
                log.warn("DB에 환율 데이터가 없습니다: {} ({} days) - 스케줄러 동작 확인 필요", currencyCode, days);
            }
            
            return dbData;
            
        } catch (Exception e) {
            log.error("DB 환율 조회 중 오류 발생: {}, {} days", currencyCode, days, e);
            return new ArrayList<>(); // 빈 리스트 반환
        }
    }
    
    
    /**
     * DB로부터 환율 히스토리 데이터 조회
     */
    private List<ExchangeChartResponseDTO> getHistoricalDataFromDatabase(String currencyCode, int days) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);
            
            List<ExchangeRateHistory> historyList = historyRepository.findByPeriod(currencyCode, startDate, endDate);
            
            List<ExchangeChartResponseDTO> chartData = historyList.stream()
                    .map(history -> ExchangeChartResponseDTO.builder()
                            .date(history.getBaseDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .rate(history.getExchangeRate())
                            .timestamp(history.getBaseDate().atStartOfDay())
                            .build())
                    .toList();
            
            log.info("DB 환율 데이터 조회 완료: {} ({} days, {} entries)", 
                    currencyCode, days, chartData.size());
            
            // DB에도 데이터가 없는 경우 최근 7일 데이터로 재시도
            if (chartData.isEmpty() && days > 7) {
                log.info("요청된 기간({} days)에 데이터가 없어 최근 7일 데이터로 재시도: {}", days, currencyCode);
                LocalDate recentStartDate = endDate.minusDays(6);
                List<ExchangeRateHistory> recentHistoryList = historyRepository.findByPeriod(currencyCode, recentStartDate, endDate);
                
                chartData = recentHistoryList.stream()
                        .map(history -> ExchangeChartResponseDTO.builder()
                                .date(history.getBaseDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .rate(history.getExchangeRate())
                                .timestamp(history.getBaseDate().atStartOfDay())
                                .build())
                        .toList();
                        
                if (!chartData.isEmpty()) {
                    log.info("최근 7일 데이터 조회 성공: {} ({} entries)", currencyCode, chartData.size());
                }
            }
            
            // 여전히 데이터가 없는 경우
            if (chartData.isEmpty()) {
                log.warn("DB에서도 환율 데이터를 찾을 수 없습니다: {} ({} days)", currencyCode, days);
            }
            
            return chartData;
            
        } catch (Exception e) {
            log.error("DB에서 환율 히스토리 조회 실패: {}, {} days", currencyCode, days, e);
            return new ArrayList<>(); // 빈 리스트 반환
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
            case "CNY" -> Constants.Exchange.CNH_CODE;       // 중국 위안 (수출입은행에서는 CNH 사용)
            default -> standardCode;
        };
    }
    
    /**
     * 수출입은행 API 응답 에러 처리
     * @return API 한도 초과 여부
     */
    private boolean handleKoreaEximApiResponse(JsonNode responseData) {
        // 배열 응답에서 개별 아이템의 result 체크 (에러 응답만 처리)
        if (responseData.isArray() && responseData.size() > 0) {
            JsonNode firstItem = responseData.get(0);
            if (firstItem.has("result")) {
                int result = firstItem.get("result").asInt();
                
                switch (result) {
                    case 1 -> {
                        log.debug("수출입은행 API 정상 응답");  // 성공
                        return false;
                    }
                    case 2 -> throw new CustomException(ErrorCode.INVALID_REQUEST, 
                        "DATA 코드 오류 (" + Constants.Api.KOREA_EXIM_DATA_CODE + " 확인)");
                    case 3 -> throw new CustomException(ErrorCode.INVALID_REQUEST, 
                        "인증키 오류. 발급받은 인증키를 확인해주세요.");
                    case 4 -> {
                        log.warn("일일 호출 한도({})를 초과했습니다. DB 데이터로 폴백합니다.", Constants.Api.KOREA_EXIM_DAILY_LIMIT);
                        return true; // 한도 초과
                    }
                    default -> {
                        log.warn("알 수 없는 수출입은행 API 응답: {}", result);
                        return false;
                    }
                }
            }
        }
        // 빈 배열은 여기서 예외를 던지지 않음 - 호출하는 곳에서 fallback 처리
        return false;
    }
    
    /**
     * 환율 데이터 새로고침 (스케줄러용)
     * - 기존 메소드 호출하여 캐시 갱신 유도
     */
    @Override
    public void refreshExchangeRates() {
        log.info("환율 데이터 새로고침 시작");
        try {
            // 기존 캐시된 메소드를 호출하여 최신 데이터 가져오기
            getAllExchangeRates();
            log.info("환율 데이터 새로고침 완료");
        } catch (Exception e) {
            log.error("환율 데이터 새로고침 실패", e);
            throw e;
        }
    }
    
    /**
     * DB에서 특정 통화의 최신 환율 조회
     */
    private ExchangeRate getLatestExchangeRateFromDB(String currencyCode) {
        try {
            Optional<ExchangeRate> latestRate = exchangeRateRepository.findLatestByCurrencyCode(currencyCode);
            return latestRate.orElse(null);
        } catch (Exception e) {
            log.error("DB에서 최신 환율 조회 실패: {}", currencyCode, e);
            return null;
        }
    }
    
    /**
     * DB에서 특정 통화의 전일 환율 조회
     */
    private ExchangeRateHistory getPreviousDayRateFromDB(String currencyCode, LocalDate currentDate) {
        try {
            Optional<ExchangeRateHistory> previousRate = historyRepository.findPreviousDayRate(currencyCode, currentDate);
            return previousRate.orElse(null);
        } catch (Exception e) {
            log.error("DB에서 전일 환율 조회 실패: {}", currencyCode, e);
            return null;
        }
    }
    
    /**
     * DB에서 전체 환율 데이터 조회 (최신 날짜 기준)
     */
    private List<ExchangeResponseDTO> getAllExchangeRatesFromDatabase() {
        try {
            List<ExchangeRate> latestRates = exchangeRateRepository.findAllLatestRates();
            
            if (latestRates.isEmpty()) {
                log.warn("DB에 환율 데이터가 없습니다. 스케줄러 동작 확인 필요");
                return new ArrayList<>();
            }
            
            List<ExchangeResponseDTO> exchangeRates = new ArrayList<>();
            
            for (ExchangeRate rate : latestRates) {
                try {
                    // ExchangeList에서 해당 통화 정보 찾기
                    ExchangeList.ExchangeType currency = ExchangeList.ExchangeType.valueOf(rate.getCurrencyCode().toUpperCase());
                    
                    ExchangeResponseDTO dto = ExchangeResponseDTO.builder()
                            .currencyCode(rate.getCurrencyCode())
                            .currencyName(currency.getLabel())
                            .flagImageUrl(currency.getFlagImageUrl())
                            .exchangeRate(rate.getExchangeRate())
                            .baseDate(rate.getBaseDate())
                            .build();
                    
                    exchangeRates.add(dto);
                } catch (IllegalArgumentException e) {
                    log.warn("지원하지 않는 통화 코드 건너뜀: {}", rate.getCurrencyCode());
                }
            }
            
            log.info("DB에서 환율 데이터 조회 완료: {} entries", exchangeRates.size());
            return exchangeRates;
            
        } catch (Exception e) {
            log.error("DB에서 전체 환율 조회 중 오류 발생", e);
            throw e;
        }
    }
    
}