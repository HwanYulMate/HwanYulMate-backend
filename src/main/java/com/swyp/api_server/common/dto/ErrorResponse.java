package com.swyp.api_server.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.swyp.api_server.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 에러 응답 통일 형식 DTO
 * - 모든 에러 응답을 일관된 형태로 반환
 * - Swagger 문서화 지원
 */
@Schema(description = "에러 응답 DTO")
@Getter
@Builder
public class ErrorResponse {
    
    @Schema(description = "성공 여부", example = "false")
    private final boolean success;
    
    @Schema(description = "에러 코드", example = "USER_001")
    private final String code;
    
    @Schema(description = "에러 메시지", example = "존재하지 않는 사용자입니다.")
    private final String message;
    
    @Schema(description = "상세 정보 (선택사항)", example = "email: user@example.com")
    private final String detail;
    
    @Schema(description = "발생 시간", example = "2024-01-15T10:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;
    
    @Schema(description = "요청 경로", example = "/api/login")
    private final String path;
    
    /**
     * ErrorCode를 사용한 ErrorResponse 생성
     * @param errorCode 에러 코드
     * @param path 요청 경로
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .detail(null)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    
    /**
     * ErrorCode와 상세 정보를 사용한 ErrorResponse 생성
     * @param errorCode 에러 코드
     * @param detail 상세 정보
     * @param path 요청 경로
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(ErrorCode errorCode, String detail, String path) {
        return ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    
    /**
     * 일반적인 Exception을 위한 ErrorResponse 생성
     * @param code 에러 코드
     * @param message 에러 메시지
     * @param path 요청 경로
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .detail(null)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}