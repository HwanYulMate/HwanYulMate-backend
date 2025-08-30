package com.swyp.api_server.domain.feedback.service;

import com.swyp.api_server.domain.feedback.dto.FeedbackRequestDto;

public interface FeedbackService {
    void sendFeedback(String userEmail, FeedbackRequestDto feedbackRequest);
}