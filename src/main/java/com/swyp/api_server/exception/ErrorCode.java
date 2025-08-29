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
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_003", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_004", "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_005", "요청한 리소스를 찾을 수 없습니다."),

    // 사용자 관련 에러 (USER_xxx)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 사용자입니다."),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER_002", "이미 존재하는 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_003", "비밀번호가 일치하지 않습니다."),
    USER_REGISTRATION_FAILED(HttpStatus.BAD_REQUEST, "USER_004", "회원가입에 실패했습니다."),

    // JWT 토큰 관련 에러 (TOKEN_xxx)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN_001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN_002", "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "TOKEN_003", "토큰이 없습니다."),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "TOKEN_004", "잘못된 토큰 타입입니다."),

    // OAuth 관련 에러 (OAUTH_xxx)
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "OAUTH_001", "지원하지 않는 소셜 로그인 제공자입니다."),
    OAUTH_USER_INFO_FAILED(HttpStatus.BAD_REQUEST, "OAUTH_002", "소셜 로그인 사용자 정보 조회에 실패했습니다."),
    OAUTH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "OAUTH_003", "유효하지 않은 OAuth 토큰입니다."),
    OAUTH_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "OAUTH_004", "소셜 로그인에 실패했습니다."),

    // 환율 관련 에러 (RATE_xxx)
    EXCHANGE_RATE_NOT_FOUND(HttpStatus.NOT_FOUND, "RATE_001", "환율 정보를 찾을 수 없습니다."),
    EXCHANGE_RATE_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "RATE_002", "환율 정보 조회 서비스에 오류가 발생했습니다."),
    INVALID_CURRENCY_CODE(HttpStatus.BAD_REQUEST, "RATE_003", "유효하지 않은 통화 코드입니다."),
    INVALID_EXCHANGE_AMOUNT(HttpStatus.BAD_REQUEST, "RATE_004", "환전 금액이 유효하지 않습니다."),
    EXCHANGE_AMOUNT_BELOW_MINIMUM(HttpStatus.BAD_REQUEST, "RATE_005", "최소 환전 금액 미달입니다."),
    EXCHANGE_AMOUNT_ABOVE_MAXIMUM(HttpStatus.BAD_REQUEST, "RATE_006", "최대 환전 금액 초과입니다."),
    UNSUPPORTED_BANK(HttpStatus.BAD_REQUEST, "RATE_007", "지원하지 않는 은행입니다."),

    // 뉴스 관련 에러 (NEWS_xxx)
    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "NEWS_001", "뉴스 정보를 찾을 수 없습니다."),
    NEWS_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "NEWS_002", "뉴스 조회 서비스에 오류가 발생했습니다."),
    NEWS_SEARCH_KEYWORD_EMPTY(HttpStatus.BAD_REQUEST, "NEWS_003", "검색 키워드를 입력해주세요."),
    NEWS_INVALID_PAGE_PARAMETER(HttpStatus.BAD_REQUEST, "NEWS_004", "페이지 번호가 유효하지 않습니다."),
    NEWS_INVALID_SIZE_PARAMETER(HttpStatus.BAD_REQUEST, "NEWS_005", "페이지 크기가 유효하지 않습니다."),
    NEWS_API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "NEWS_006", "뉴스 API 호출 한도를 초과했습니다."),
    NEWS_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "NEWS_007", "뉴스 API 인증에 실패했습니다."),

    // 알림 관련 에러 (ALERT_xxx)
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "ALERT_001", "알림 설정을 찾을 수 없습니다."),
    ALERT_CREATION_FAILED(HttpStatus.BAD_REQUEST, "ALERT_002", "알림 설정 생성에 실패했습니다."),
    INVALID_ALERT_CONDITION(HttpStatus.BAD_REQUEST, "ALERT_003", "유효하지 않은 알림 조건입니다."),
    
    // Redis 캐시 관련 에러 (CACHE_xxx)
    CACHE_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "CACHE_001", "캐시 서버 연결에 실패했습니다."),
    CACHE_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CACHE_002", "캐시 작업 중 오류가 발생했습니다."),
    CACHE_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CACHE_003", "캐시 데이터 직렬화에 실패했습니다."),
    CACHE_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "CACHE_004", "캐시 요청 시간이 초과되었습니다.");

    private final HttpStatus httpStatus;        // HTTP 응답 상태 코드
    private final String code;                  // 비즈니스 에러 코드
    private final String message;               // 사용자에게 표시할 메시지
}