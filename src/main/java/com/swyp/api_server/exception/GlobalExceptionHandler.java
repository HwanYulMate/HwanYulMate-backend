package com.swyp.api_server.exception;

import com.swyp.api_server.common.dto.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * - 애플리케이션 전체에서 발생하는 예외를 통일된 형태로 처리
 * - CustomException과 일반적인 예외들을 각각 적절히 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     * - 비즈니스 로직에서 발생하는 사용자 정의 예외 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(
            CustomException ex, HttpServletRequest request) {
        
        log.warn("CustomException occurred: code={}, message={}, path={}", 
                ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ex.getDetail() != null 
            ? ErrorResponse.of(ex.getErrorCode(), ex.getDetail(), request.getRequestURI())
            : ErrorResponse.of(ex.getErrorCode(), request.getRequestURI());
            
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    /**
     * JWT 관련 예외 처리
     * - JWT 토큰 파싱, 검증 실패 시 발생하는 예외 처리
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex, HttpServletRequest request) {
        
        log.warn("JWT Exception occurred: message={}, path={}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_TOKEN, 
                ex.getMessage(), 
                request.getRequestURI()
        );
        
        return ResponseEntity
                .status(ErrorCode.INVALID_TOKEN.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * Bean Validation 예외 처리 (@Valid 어노테이션 검증 실패)
     * - 요청 DTO의 유효성 검사 실패 시 발생
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(
            BindException ex, HttpServletRequest request) {
        
        log.warn("Validation failed: path={}", request.getRequestURI());
        
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST,
                detail,
                request.getRequestURI()
        );
        
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * 메서드 인자 타입 불일치 예외 처리
     * - URL 경로 변수나 요청 파라미터의 타입이 맞지 않을 때 발생
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        log.warn("Method argument type mismatch: parameter={}, value={}, path={}", 
                ex.getName(), ex.getValue(), request.getRequestURI());
        
        String detail = String.format("파라미터 '%s'의 값 '%s'이(가) 올바르지 않습니다.", 
                ex.getName(), ex.getValue());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST,
                detail,
                request.getRequestURI()
        );
        
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * IllegalArgumentException 처리
     * - 잘못된 인자 전달 시 발생하는 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        log.warn("IllegalArgumentException occurred: message={}, path={}", 
                ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * 기타 모든 예외 처리
     * - 위에서 처리되지 않은 모든 예외에 대한 기본 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected exception occurred: message={}, path={}", 
                ex.getMessage(), request.getRequestURI(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );
        
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(errorResponse);
    }
}