package com.swyp.api_server.common.validator;

import com.swyp.api_server.common.constants.Constants;
import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.entity.User;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 공통 검증 로직을 제공하는 유틸리티 클래스
 * - 중복된 검증 로직을 중앙화하여 코드 재사용성 향상
 * - 일관된 검증 기준 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonValidator {
    
    private final UserRepository userRepository;
    
    // 이메일 정규식 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    /**
     * 사용자 이메일로 사용자 조회 및 검증
     * @param email 사용자 이메일
     * @return 조회된 사용자 엔티티
     * @throws CustomException 사용자를 찾을 수 없는 경우
     */
    public User validateAndGetUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이메일이 비어있습니다.");
        }
        
        return userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("사용자를 찾을 수 없습니다: {}", email);
                return new CustomException(ErrorCode.USER_NOT_FOUND, "이메일: " + email);
            });
    }
    
    /**
     * 사용자 ID로 사용자 조회 및 검증
     * @param userId 사용자 ID
     * @return 조회된 사용자 엔티티
     * @throws CustomException 사용자를 찾을 수 없는 경우
     */
    public User validateAndGetUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "유효하지 않은 사용자 ID입니다.");
        }
        
        return userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("사용자를 찾을 수 없습니다: {}", userId);
                return new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 ID: " + userId);
            });
    }
    
    /**
     * 통화 코드 유효성 검증
     * @param currencyCode 통화 코드 (USD, EUR, JPY 등)
     * @throws CustomException 유효하지 않은 통화 코드인 경우
     */
    public void validateCurrencyCode(String currencyCode) {
        if (!StringUtils.hasText(currencyCode)) {
            throw new CustomException(ErrorCode.INVALID_CURRENCY_CODE, "통화 코드가 비어있습니다.");
        }
        
        String normalizedCode = currencyCode.trim().toUpperCase();
        
        try {
            ExchangeList.ExchangeType.valueOf(normalizedCode);
            log.debug("유효한 통화 코드 검증 완료: {}", normalizedCode);
        } catch (IllegalArgumentException e) {
            log.warn("지원하지 않는 통화 코드: {}", currencyCode);
            throw new CustomException(ErrorCode.INVALID_CURRENCY_CODE, 
                "지원하지 않는 통화 코드입니다: " + currencyCode);
        }
    }
    
    /**
     * 페이지네이션 파라미터 검증
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @throws CustomException 유효하지 않은 페이지 파라미터인 경우
     */
    public void validatePageParams(int page, int size) {
        if (page < Constants.Pagination.MIN_PAGE_NUMBER) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                "페이지 번호는 " + Constants.Pagination.MIN_PAGE_NUMBER + " 이상이어야 합니다.");
        }
        
        if (size < Constants.Pagination.MIN_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                "페이지 크기는 " + Constants.Pagination.MIN_PAGE_SIZE + " 이상이어야 합니다.");
        }
        
        if (size > Constants.Pagination.MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                "페이지 크기는 " + Constants.Pagination.MAX_PAGE_SIZE + " 이하여야 합니다.");
        }
        
        log.debug("페이지 파라미터 검증 완료: page={}, size={}", page, size);
    }
    
    /**
     * 뉴스 페이지네이션 파라미터 검증 (뉴스 전용)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @throws CustomException 유효하지 않은 파라미터인 경우
     */
    public void validateNewsPageParams(int page, int size) {
        if (page < 0) {
            throw new CustomException(ErrorCode.NEWS_INVALID_PAGE_PARAMETER, 
                "페이지 번호는 0 이상이어야 합니다.");
        }
        
        if (size < 1) {
            throw new CustomException(ErrorCode.NEWS_INVALID_SIZE_PARAMETER, 
                "페이지 크기는 1 이상이어야 합니다.");
        }
        
        if (size > Constants.News.MAX_NEWS_SIZE) {
            throw new CustomException(ErrorCode.NEWS_INVALID_SIZE_PARAMETER, 
                "페이지 크기는 " + Constants.News.MAX_NEWS_SIZE + " 이하여야 합니다.");
        }
        
        log.debug("뉴스 페이지 파라미터 검증 완료: page={}, size={}", page, size);
    }
    
    /**
     * 이메일 형식 검증
     * @param email 검증할 이메일 주소
     * @throws CustomException 유효하지 않은 이메일 형식인 경우
     */
    public void validateEmailFormat(String email) {
        if (!StringUtils.hasText(email)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이메일이 비어있습니다.");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                "유효하지 않은 이메일 형식입니다: " + email);
        }
        
        log.debug("이메일 형식 검증 완료: {}", email);
    }
    
    /**
     * FCM 토큰 유효성 검증
     * @param token FCM 토큰
     * @throws CustomException 유효하지 않은 토큰인 경우
     */
    public void validateFcmToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "FCM 토큰이 비어있습니다.");
        }
        
        String trimmedToken = token.trim();
        
        // FCM 토큰은 일반적으로 152자 이상
        if (trimmedToken.length() < 50) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "유효하지 않은 FCM 토큰 형식입니다.");
        }
        
        log.debug("FCM 토큰 검증 완료: 길이={}", trimmedToken.length());
    }
    
    /**
     * 환율 값 유효성 검증
     * @param rate 환율 값
     * @throws CustomException 유효하지 않은 환율 값인 경우
     */
    public void validateExchangeRate(Double rate) {
        if (rate == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "환율 값이 null입니다.");
        }
        
        if (rate <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "환율 값은 0보다 커야 합니다.");
        }
        
        if (rate > 1000000) { // 상식적인 최대 환율 한도
            throw new CustomException(ErrorCode.INVALID_REQUEST, "환율 값이 비정상적으로 큽니다.");
        }
        
        log.debug("환율 값 검증 완료: {}", rate);
    }
    
    /**
     * 문자열 길이 검증
     * @param value 검증할 문자열
     * @param fieldName 필드명 (오류 메시지용)
     * @param minLength 최소 길이
     * @param maxLength 최대 길이
     * @throws CustomException 길이가 범위를 벗어나는 경우
     */
    public void validateStringLength(String value, String fieldName, int minLength, int maxLength) {
        if (!StringUtils.hasText(value)) {
            if (minLength > 0) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, 
                    fieldName + "이(가) 비어있습니다.");
            }
            return;
        }
        
        int length = value.trim().length();
        
        if (length < minLength) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                fieldName + "은(는) " + minLength + "자 이상이어야 합니다.");
        }
        
        if (length > maxLength) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                fieldName + "은(는) " + maxLength + "자 이하여야 합니다.");
        }
        
        log.debug("{} 길이 검증 완료: {}", fieldName, length);
    }
}