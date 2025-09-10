package com.swyp.api_server.domain.feedback.controller;

import com.swyp.api_server.common.util.AuthUtil;
import com.swyp.api_server.domain.feedback.dto.FeedbackRequestDto;
import com.swyp.api_server.domain.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.swyp.api_server.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 피드백 관리 컨트롤러
 * - 사용자 피드백 전송 기능 제공
 */
@Tag(name = "Feedback", description = "피드백 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    private final AuthUtil authUtil;

    /**
     * 피드백 보내기 API
     * @param feedbackRequest 피드백 요청 데이터
     * @param request HTTP 요청
     * @return 전송 결과
     */
    @Operation(summary = "피드백 보내기", 
        description = "사용자의 피드백(버그 신고, 기능 제안, 문의사항 등)을 담당자 이메일로 전송합니다.",
        security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "피드백 전송 성공",
            content = @Content(examples = @ExampleObject(value = "피드백이 전송되었습니다. 소중한 의견 감사합니다!"))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "피드백 전송 실패 (잘못된 유형, 내용 길이 초과, 빈 내용 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "사용자를 찾을 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/feedback")
    public ResponseEntity<?> sendFeedback(
            @RequestBody FeedbackRequestDto feedbackRequest, 
            HttpServletRequest request) {
        
        String userEmail = authUtil.extractUserEmail(request);
        feedbackService.sendFeedback(userEmail, feedbackRequest);
        return ResponseEntity.ok("피드백이 전송되었습니다. 소중한 의견 감사합니다!");
    }
    
    /**
     * 피드백 유형 조회 API (참고용)
     * @return 사용 가능한 피드백 유형 목록
     */
    @Operation(summary = "피드백 유형 조회", 
        description = "사용 가능한 피드백 유형 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "피드백 유형 조회 성공",
            content = @Content(examples = @ExampleObject(
                name = "피드백 유형 목록",
                value = "{\"bug\": \"버그 신고\", \"suggestion\": \"기능 제안\", \"question\": \"문의사항\", \"other\": \"기타 피드백\"}"))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/feedback/types")
    public ResponseEntity<?> getFeedbackTypes() {
        java.util.Map<String, String> feedbackTypes = new java.util.HashMap<>();
        feedbackTypes.put("bug", "버그 신고");
        feedbackTypes.put("suggestion", "기능 제안"); 
        feedbackTypes.put("question", "문의사항");
        feedbackTypes.put("other", "기타 피드백");
        
        return ResponseEntity.ok(feedbackTypes);
    }
}