package com.swyp.api_server.domain.feedback.service;

import com.swyp.api_server.domain.feedback.dto.FeedbackRequestDto;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 피드백 서비스 구현
 * - 사용자 피드백을 이메일로 담당자에게 전송
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final JavaMailSender mailSender;
    
    @Value("${feedback.admin-email}")
    private String adminEmail;
    
    @Value("${spring.mail.username:noreply@hwanyulmate.com}")
    private String fromEmail;

    @Override
    public void sendFeedback(String userEmail, FeedbackRequestDto feedbackRequest) {
        // 유효성 검증
        validateFeedback(feedbackRequest);
        
        try {
            // 이메일 전송
            SimpleMailMessage message = createFeedbackEmail(userEmail, feedbackRequest);
            mailSender.send(message);
            
            log.info("피드백 전송 성공: 사용자={}, 유형={}", userEmail, feedbackRequest.getType());
            
        } catch (Exception e) {
            log.error("피드백 전송 실패: 사용자={}, 오류={}", userEmail, e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "피드백 전송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
    
    /**
     * 피드백 유효성 검증
     */
    private void validateFeedback(FeedbackRequestDto feedbackRequest) {
        if (!feedbackRequest.isValidType()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                    "올바른 피드백 유형을 선택해주세요. (bug, suggestion, question, other)");
        }
        
        if (!feedbackRequest.isValidContent()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                    "피드백 내용은 1자 이상 2000자 이하로 입력해주세요.");
        }
    }
    
    /**
     * 피드백 이메일 생성
     */
    private SimpleMailMessage createFeedbackEmail(String userEmail, FeedbackRequestDto feedbackRequest) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject(createSubject(feedbackRequest.getType()));
        message.setText(createEmailContent(userEmail, feedbackRequest));
        
        return message;
    }
    
    /**
     * 피드백 유형별 제목 생성
     */
    private String createSubject(String type) {
        String typeKorean = switch (type) {
            case "bug" -> "버그 신고";
            case "suggestion" -> "기능 제안";
            case "question" -> "문의사항";
            case "other" -> "기타 피드백";
            default -> "피드백";
        };
        
        return String.format("[HwanYulMate] %s - %s", 
                typeKorean, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }
    
    /**
     * 이메일 본문 생성
     */
    private String createEmailContent(String userEmail, FeedbackRequestDto feedbackRequest) {
        StringBuilder content = new StringBuilder();
        
        content.append("===== HwanYulMate 사용자 피드백 =====\n\n");
        
        content.append("📧 사용자 이메일: ").append(userEmail).append("\n");
        content.append("📝 피드백 유형: ").append(getFeedbackTypeName(feedbackRequest.getType())).append("\n");
        content.append("📅 접수 시간: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        if (feedbackRequest.getDeviceInfo() != null && !feedbackRequest.getDeviceInfo().trim().isEmpty()) {
            content.append("📱 기기 정보: ").append(feedbackRequest.getDeviceInfo()).append("\n");
        }
        
        content.append("\n===== 피드백 내용 =====\n");
        content.append(feedbackRequest.getContent());
        
        content.append("\n\n===== 처리 안내 =====\n");
        content.append("• 본 피드백은 개발팀에서 검토 후 개선에 반영됩니다.\n");
        content.append("• 긴급한 문의사항의 경우 별도 연락을 통해 빠른 응답을 제공할 수 있습니다.\n");
        content.append("• 감사합니다!\n\n");
        
        content.append("---\n");
        content.append("HwanYulMate 개발팀\n");
        content.append("이 메일은 자동 생성된 메일입니다.");
        
        return content.toString();
    }
    
    /**
     * 피드백 유형 한글명 반환
     */
    private String getFeedbackTypeName(String type) {
        return switch (type) {
            case "bug" -> "버그 신고";
            case "suggestion" -> "기능 제안";
            case "question" -> "문의사항";
            case "other" -> "기타 피드백";
            default -> type;
        };
    }
}