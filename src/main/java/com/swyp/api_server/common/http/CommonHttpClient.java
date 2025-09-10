package com.swyp.api_server.common.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.api_server.common.constants.Constants;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 공통 HTTP 클라이언트 서비스
 * - OkHttp 기반 HTTP 요청 처리 중앙화
 * - 재시도 로직 및 에러 핸들링 통일
 * - JSON 응답 파싱 자동화
 */
@Slf4j
@Service
public class CommonHttpClient {
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public CommonHttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.Api.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(Constants.Api.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(Constants.Api.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
    
    /**
     * GET 요청으로 JSON 응답을 JsonNode로 반환
     * @param url 요청 URL
     * @return JsonNode 응답 데이터
     * @throws CustomException API 호출 실패 시
     */
    public JsonNode getJson(String url) {
        return getJson(url, Map.of());
    }
    
    /**
     * GET 요청으로 JSON 응답을 JsonNode로 반환 (헤더 포함)
     * @param url 요청 URL
     * @param headers 요청 헤더
     * @return JsonNode 응답 데이터
     * @throws CustomException API 호출 실패 시
     */
    public JsonNode getJson(String url, Map<String, String> headers) {
        String response = get(url, headers);
        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("JSON 파싱 실패: url={}, response={}", url, response, e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
                "API 응답 JSON 파싱 실패", e);
        }
    }
    
    /**
     * GET 요청으로 특정 타입의 객체 반환
     * @param url 요청 URL
     * @param responseType 응답 타입 클래스
     * @param <T> 응답 타입
     * @return 파싱된 응답 객체
     * @throws CustomException API 호출 실패 시
     */
    public <T> Optional<T> get(String url, Class<T> responseType) {
        return get(url, Map.of(), responseType);
    }
    
    /**
     * GET 요청으로 특정 타입의 객체 반환 (헤더 포함)
     * @param url 요청 URL
     * @param headers 요청 헤더
     * @param responseType 응답 타입 클래스
     * @param <T> 응답 타입
     * @return 파싱된 응답 객체
     * @throws CustomException API 호출 실패 시
     */
    public <T> Optional<T> get(String url, Map<String, String> headers, Class<T> responseType) {
        try {
            String response = get(url, headers);
            T result = objectMapper.readValue(response, responseType);
            return Optional.ofNullable(result);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("응답 객체 변환 실패: url={}, type={}", url, responseType.getSimpleName(), e);
            return Optional.empty();
        }
    }
    
    /**
     * GET 요청으로 원시 문자열 응답 반환
     * @param url 요청 URL
     * @return 응답 문자열
     * @throws CustomException API 호출 실패 시
     */
    public String get(String url) {
        return get(url, Map.of());
    }
    
    /**
     * GET 요청으로 원시 문자열 응답 반환 (헤더 포함)
     * @param url 요청 URL
     * @param headers 요청 헤더
     * @return 응답 문자열
     * @throws CustomException API 호출 실패 시
     */
    public String get(String url, Map<String, String> headers) {
        return executeWithRetry(url, "GET", headers, null, 0);
    }
    
    /**
     * POST 요청 실행
     * @param url 요청 URL
     * @param requestBody 요청 본문 (JSON 문자열)
     * @return 응답 문자열
     * @throws CustomException API 호출 실패 시
     */
    public String post(String url, String requestBody) {
        return post(url, Map.of(), requestBody);
    }
    
    /**
     * POST 요청 실행 (헤더 포함)
     * @param url 요청 URL
     * @param headers 요청 헤더
     * @param requestBody 요청 본문
     * @return 응답 문자열
     * @throws CustomException API 호출 실패 시
     */
    public String post(String url, Map<String, String> headers, String requestBody) {
        return executeWithRetry(url, "POST", headers, requestBody, 0);
    }
    
    /**
     * 재시도 로직이 포함된 HTTP 요청 실행
     * @param url 요청 URL
     * @param method HTTP 메서드
     * @param headers 요청 헤더
     * @param requestBody 요청 본문 (POST인 경우)
     * @param retryCount 현재 재시도 횟수
     * @return 응답 문자열
     * @throws CustomException API 호출 실패 시
     */
    private String executeWithRetry(String url, String method, Map<String, String> headers, 
                                  String requestBody, int retryCount) {
        try {
            Request.Builder requestBuilder = new Request.Builder().url(url);
            
            // 헤더 추가
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(requestBuilder::addHeader);
            }
            
            // 기본 헤더 설정
            requestBuilder.addHeader("Accept", "application/json");
            requestBuilder.addHeader("User-Agent", "API-Server/1.0");
            
            // HTTP 메서드별 처리
            if ("POST".equals(method) && StringUtils.hasText(requestBody)) {
                RequestBody body = RequestBody.create(requestBody, 
                    MediaType.parse("application/json; charset=utf-8"));
                requestBuilder.post(body);
            }
            
            Request request = requestBuilder.build();
            
            log.debug("HTTP 요청 시작: {} {}, 재시도={}", method, url, retryCount);
            
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    log.debug("HTTP 요청 성공: {} {}, 응답길이={}", 
                        method, url, responseBody.length());
                    return responseBody;
                }
                
                // HTTP 에러 상태 코드 처리
                return handleHttpError(url, method, response, responseBody, retryCount);
            }
            
        } catch (IOException e) {
            return handleIOException(url, method, e, retryCount);
        } catch (Exception e) {
            log.error("HTTP 요청 중 예상치 못한 오류: {} {}, 재시도={}", 
                method, url, retryCount, e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
                "HTTP 요청 실행 중 오류: " + e.getMessage(), e);
        }
    }
    
    /**
     * HTTP 에러 응답 처리
     */
    private String handleHttpError(String url, String method, Response response, 
                                 String responseBody, int retryCount) {
        int statusCode = response.code();
        
        log.warn("HTTP 요청 실패: {} {}, 상태코드={}, 응답={}, 재시도={}", 
            method, url, statusCode, responseBody, retryCount);
        
        // 재시도 가능한 에러 (5xx 서버 에러, 429 Too Many Requests)
        if (retryCount < Constants.Api.MAX_RETRY_COUNT && 
           (statusCode >= 500 || statusCode == 429)) {
            
            log.info("HTTP 요청 재시도 예정: {} {}, 재시도={}/{}", 
                method, url, retryCount + 1, Constants.Api.MAX_RETRY_COUNT);
                
            try {
                // 재시도 간격 (1초, 2초, 3초)
                Thread.sleep(Constants.Api.RETRY_BASE_DELAY_MS * (retryCount + 1));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            return executeWithRetry(url, method, Map.of(), null, retryCount + 1);
        }
        
        // 재시도 불가능한 에러 또는 최대 재시도 횟수 도달
        String errorMessage = String.format("HTTP %d 에러: %s", statusCode, responseBody);
        
        if (statusCode == 400) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, errorMessage);
        } else if (statusCode == 401) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, errorMessage);
        } else if (statusCode == 404) {
            throw new CustomException(ErrorCode.NOT_FOUND, errorMessage);
        } else if (statusCode == 429) {
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
                "API 호출 한도 초과: " + errorMessage);
        } else {
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, errorMessage);
        }
    }
    
    /**
     * IO 예외 처리
     */
    private String handleIOException(String url, String method, IOException e, int retryCount) {
        log.warn("HTTP 요청 IO 오류: {} {}, 오류={}, 재시도={}", 
            method, url, e.getMessage(), retryCount);
        
        // 네트워크 오류는 재시도 가능
        if (retryCount < Constants.Api.MAX_RETRY_COUNT) {
            log.info("네트워크 오류로 HTTP 요청 재시도: {} {}, 재시도={}/{}", 
                method, url, retryCount + 1, Constants.Api.MAX_RETRY_COUNT);
                
            try {
                Thread.sleep(Constants.Api.RETRY_BASE_DELAY_MS * (retryCount + 1));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            return executeWithRetry(url, method, Map.of(), null, retryCount + 1);
        }
        
        // 최대 재시도 횟수 도달
        throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
            "네트워크 오류로 HTTP 요청 최종 실패: " + e.getMessage(), e);
    }
    
    /**
     * URL 파라미터를 포함한 URL 생성
     * @param baseUrl 기본 URL
     * @param params URL 파라미터
     * @return 완성된 URL
     */
    public String buildUrl(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
        params.forEach(urlBuilder::addQueryParameter);
        
        return urlBuilder.build().toString();
    }
}