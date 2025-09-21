package com.swyp.api_server.domain.rate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 서비스 시작일 추적 서비스
 * - 히스토리 확장 스케줄링을 위한 기준일 관리
 */
@Slf4j
@Service
public class ServiceStartTracker {

    // 서비스 시작일 (오늘부터 히스토리 확장 시작)
    private static final String SERVICE_START_DATE = "2025-09-21"; // 오늘 배포일
    
    /**
     * 서비스 시작일 조회
     */
    public LocalDate getServiceStartDate() {
        return LocalDate.parse(SERVICE_START_DATE, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    
    /**
     * 서비스 시작 후 경과 일수 계산
     */
    public long getDaysSinceStart() {
        LocalDate startDate = getServiceStartDate();
        LocalDate today = LocalDate.now();
        return startDate.until(today).getDays();
    }
    
    /**
     * 90일 확장 시점 도달 여부 (7일 후)
     */
    public boolean shouldExpandTo90Days() {
        long daysSinceStart = getDaysSinceStart();
        boolean shouldExpand = daysSinceStart >= 7;
        
        if (shouldExpand) {
            log.info("90일 확장 조건 충족: 서비스 시작 후 {} 일 경과", daysSinceStart);
        }
        
        return shouldExpand;
    }
    
    /**
     * 180일 확장 시점 도달 여부 (30일 후)
     */
    public boolean shouldExpandTo180Days() {
        long daysSinceStart = getDaysSinceStart();
        boolean shouldExpand = daysSinceStart >= 30;
        
        if (shouldExpand) {
            log.info("180일 확장 조건 충족: 서비스 시작 후 {} 일 경과", daysSinceStart);
        }
        
        return shouldExpand;
    }
    
    /**
     * 365일 확장 시점 도달 여부 (90일 후)
     */
    public boolean shouldExpandTo365Days() {
        long daysSinceStart = getDaysSinceStart();
        boolean shouldExpand = daysSinceStart >= 90;
        
        if (shouldExpand) {
            log.info("365일 확장 조건 충족: 서비스 시작 후 {} 일 경과", daysSinceStart);
        }
        
        return shouldExpand;
    }
    
    /**
     * 서비스 시작일 업데이트 (운영진 전용)
     * - 실제 서비스 런칭일에 맞춰 조정
     */
    public void updateServiceStartDate(String newStartDate) {
        log.warn("서비스 시작일 변경: {} -> {}", SERVICE_START_DATE, newStartDate);
        // 실제 구현 시 DB나 Configuration에 저장
    }
}