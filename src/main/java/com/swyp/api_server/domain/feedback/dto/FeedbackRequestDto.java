package com.swyp.api_server.domain.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피드백 전송 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequestDto {
    
    private String type;        // 피드백 유형 (bug, suggestion, question, other)
    private String content;     // 피드백 내용
    private String deviceInfo;  // 기기 정보 (선택사항)
    
    /**
     * 피드백 유형 검증
     */
    public boolean isValidType() {
        if (type == null) return false;
        return type.equals("bug") || type.equals("suggestion") || 
               type.equals("question") || type.equals("other");
    }
    
    /**
     * 피드백 내용 검증
     */
    public boolean isValidContent() {
        return content != null && !content.trim().isEmpty() && content.length() <= 2000;
    }
}