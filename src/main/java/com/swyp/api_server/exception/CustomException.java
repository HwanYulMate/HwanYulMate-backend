package com.swyp.api_server.exception;

import lombok.Getter;

/**
 * 애플리케이션 비즈니스 로직에서 발생하는 사용자 정의 예외
 * - ErrorCode를 통해 통일된 에러 처리 제공
 * - RuntimeException을 상속하여 언체크 예외로 처리
 */
@Getter
public class CustomException extends RuntimeException {
    
    private final ErrorCode errorCode;      // 에러 코드 (HTTP 상태, 비즈니스 코드, 메시지 포함)
    private final String detail;            // 추가 상세 정보 (선택사항)
    
    /**
     * 기본 CustomException 생성자
     * @param errorCode 발생한 에러 코드
     */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }
    
    /**
     * 상세 정보가 있는 CustomException 생성자
     * @param errorCode 발생한 에러 코드
     * @param detail 추가 상세 정보
     */
    public CustomException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }
    
    /**
     * 원인 예외가 있는 CustomException 생성자
     * @param errorCode 발생한 에러 코드
     * @param cause 원인이 된 예외
     */
    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detail = null;
    }
    
    /**
     * 상세 정보와 원인 예외가 모두 있는 CustomException 생성자
     * @param errorCode 발생한 에러 코드
     * @param detail 추가 상세 정보
     * @param cause 원인이 된 예외
     */
    public CustomException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}