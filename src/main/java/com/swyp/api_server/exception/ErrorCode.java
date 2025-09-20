package com.swyp.api_server.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션에서 사용하는 에러 코드 정의
 * - HTTP 상태 코드와 비즈니스 에러 코드, 메시지를 통합 관리
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버에서 예상치 못한 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_002", "요청 형식이 올바르지 않습니다. 요청 데이터를 확인해주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_003", "로그인이 필요한 서비스입니다. 토큰을 확인해주세요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_004", "해당 작업을 수행할 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_005", "요청하신 페이지나 데이터를 찾을 수 없습니다."),

    // 사용자 관련 에러 (USER_xxx)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 사용자입니다. 이메일 주소를 확인해주세요."),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER_002", "이미 가입된 이메일입니다. 다른 이메일을 사용하거나 로그인을 시도해주세요."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_003", "비밀번호가 일치하지 않습니다. 올바른 비밀번호를 입력해주세요."),
    USER_REGISTRATION_FAILED(HttpStatus.BAD_REQUEST, "USER_004", "회원가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // JWT 토큰 관련 에러 (TOKEN_xxx)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN_001", "유효하지 않은 토큰입니다. 다시 로그인해주세요."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN_002", "토큰이 만료되었습니다. 다시 로그인해주세요."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "TOKEN_003", "인증 토큰이 없습니다. Authorization 헤더를 확인해주세요."),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "TOKEN_004", "올바르지 않은 토큰 형식입니다. Bearer 토큰을 사용해주세요."),

    // OAuth 관련 에러 (OAUTH_xxx)
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "OAUTH_001", "지원하지 않는 소셜 로그인입니다. Google 또는 Apple 로그인을 사용해주세요."),
    OAUTH_USER_INFO_FAILED(HttpStatus.BAD_REQUEST, "OAUTH_002", "소셜 로그인 사용자 정보를 가져올 수 없습니다. 토큰을 확인하거나 다시 시도해주세요."),
    OAUTH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "OAUTH_003", "소셜 로그인 토큰이 유효하지 않습니다. 다시 로그인해주세요."),
    OAUTH_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "OAUTH_004", "소셜 로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // 환율 관련 에러 (RATE_xxx)
    EXCHANGE_RATE_NOT_FOUND(HttpStatus.NOT_FOUND, "RATE_001", "해당 통화의 환율 정보를 찾을 수 없습니다. 통화 코드를 확인하거나 잠시 후 시도해주세요."),
    EXCHANGE_RATE_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "RATE_002", "환율 정보 조회 서비스가 일시적으로 이용할 수 없습니다. 잠시 후 다시 시도해주세요."),
    INVALID_CURRENCY_CODE(HttpStatus.BAD_REQUEST, "RATE_003", "지원하지 않는 통화 코드입니다. 지원 통화 목록을 확인해주세요. (예: USD, EUR, CNY 등)"),
    INVALID_EXCHANGE_AMOUNT(HttpStatus.BAD_REQUEST, "RATE_004", "환전 금액 형식이 올바르지 않습니다. 양수로 입력해주세요."),
    EXCHANGE_AMOUNT_BELOW_MINIMUM(HttpStatus.BAD_REQUEST, "RATE_005", "최소 환전 금액보다 적습니다. 더 큰 금액을 입력해주세요."),
    EXCHANGE_AMOUNT_ABOVE_MAXIMUM(HttpStatus.BAD_REQUEST, "RATE_006", "최대 환전 금액을 초과했습니다. 더 적은 금액을 입력해주세요."),
    UNSUPPORTED_BANK(HttpStatus.BAD_REQUEST, "RATE_007", "해당 은행은 지원하지 않습니다. 지원 은행 목록을 확인해주세요."),
    EXCHANGE_RATE_API_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "RATE_008", "환율 조회 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    EXCHANGE_FEE_EXCEEDS_AMOUNT(HttpStatus.BAD_REQUEST, "RATE_009", "수수료가 환전 금액을 초과합니다. 더 큰 금액으로 환전을 시도해주세요."),
    EXCHANGE_AMOUNT_TOO_SMALL(HttpStatus.BAD_REQUEST, "RATE_010", "환전 금액이 너무 작습니다. 수수료를 고려하여 더 큰 금액을 입력해주세요."),

    // 뉴스 관련 에러 (NEWS_xxx)
    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "NEWS_001", "요청하신 뉴스를 찾을 수 없습니다. 검색어나 조건을 확인해주세요."),
    NEWS_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "NEWS_002", "뉴스 조회 서비스가 일시적으로 이용할 수 없습니다. 잠시 후 다시 시도해주세요."),
    NEWS_SEARCH_KEYWORD_EMPTY(HttpStatus.BAD_REQUEST, "NEWS_003", "검색할 키워드를 입력해주세요. 빈 값은 허용되지 않습니다."),
    NEWS_INVALID_PAGE_PARAMETER(HttpStatus.BAD_REQUEST, "NEWS_004", "페이지 번호는 0 이상이어야 합니다. 올바른 값을 입력해주세요."),
    NEWS_INVALID_SIZE_PARAMETER(HttpStatus.BAD_REQUEST, "NEWS_005", "페이지 크기는 1~50 사이여야 합니다. 올바른 값을 입력해주세요."),
    NEWS_API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "NEWS_006", "뉴스 조회 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    NEWS_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "NEWS_007", "뉴스 API 인증에 실패했습니다. 관리자에게 문의해주세요."),

    // 알림 관련 에러 (ALERT_xxx)
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "ALERT_001", "해당 알림 설정을 찾을 수 없습니다. 알림 ID를 확인해주세요."),
    ALERT_CREATION_FAILED(HttpStatus.BAD_REQUEST, "ALERT_002", "알림 설정 생성 중 오류가 발생했습니다. 입력 정보를 확인하고 다시 시도해주세요."),
    INVALID_ALERT_CONDITION(HttpStatus.BAD_REQUEST, "ALERT_003", "알림 조건이 올바르지 않습니다. 목표 환율과 통화 코드를 확인해주세요."),
    ALERT_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "ALERT_004", "해당 통화의 알림 설정을 찾을 수 없습니다."),
    
    // Redis 캐시 관련 에러 (CACHE_xxx)
    CACHE_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "CACHE_001", "캐시 서버에 연결할 수 없습니다. 서버 상태를 확인 중입니다."),
    CACHE_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CACHE_002", "캐시 작업 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    CACHE_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CACHE_003", "캐시 데이터 처리 중 오류가 발생했습니다. 관리자에게 문의해주세요."),
    CACHE_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "CACHE_004", "캐시 서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;        // HTTP 응답 상태 코드
    private final String code;                  // 비즈니스 에러 코드
    private final String message;               // 사용자에게 표시할 메시지
}