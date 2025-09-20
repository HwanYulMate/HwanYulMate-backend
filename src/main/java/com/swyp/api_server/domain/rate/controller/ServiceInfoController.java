package com.swyp.api_server.domain.rate.controller;

import com.swyp.api_server.domain.rate.service.ServiceStartTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 서비스 정보 조회 컨트롤러
 * - 히스토리 확장 단계 및 서비스 정보 제공
 */
@Tag(name = "서비스 정보", description = "히스토리 확장 단계 및 서비스 상태 정보 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ServiceInfoController {

    private final ServiceStartTracker serviceStartTracker;

    /**
     * 현재 히스토리 데이터 확장 단계 조회
     */
    @Operation(summary = "히스토리 데이터 확장 단계 조회", 
               description = "현재 서비스의 히스토리 데이터 확장 단계를 조회합니다. " +
                           "서비스 시작일로부터 경과 일수에 따라 지원되는 차트 기간이 달라집니다.")
    @GetMapping("/service/history-status")
    public ResponseEntity<Map<String, Object>> getHistoryStatus() {
        long daysSinceStart = serviceStartTracker.getDaysSinceStart();
        
        // 현재 지원되는 최대 차트 기간 계산
        String maxSupportedPeriod;
        String nextExpansion = null;
        long daysUntilNext = 0;
        
        if (daysSinceStart >= 90) {
            maxSupportedPeriod = "1년 (365일)";
            nextExpansion = "완료";
        } else if (daysSinceStart >= 30) {
            maxSupportedPeriod = "6개월 (180일)";
            nextExpansion = "1년 확장";
            daysUntilNext = 90 - daysSinceStart;
        } else if (daysSinceStart >= 7) {
            maxSupportedPeriod = "3개월 (90일)";
            nextExpansion = "6개월 확장";
            daysUntilNext = 30 - daysSinceStart;
        } else {
            maxSupportedPeriod = "1개월 (30일)";
            nextExpansion = "3개월 확장";
            daysUntilNext = 7 - daysSinceStart;
        }
        
        return ResponseEntity.ok(Map.of(
            "serviceStartDate", serviceStartTracker.getServiceStartDate().toString(),
            "daysSinceStart", daysSinceStart,
            "maxSupportedPeriod", maxSupportedPeriod,
            "nextExpansion", nextExpansion,
            "daysUntilNext", Math.max(0, daysUntilNext),
            "expansionSchedule", Map.of(
                "30일", "서비스 시작 시",
                "90일", "시작 후 7일",
                "180일", "시작 후 30일", 
                "365일", "시작 후 90일"
            )
        ));
    }

    /**
     * 서비스 버전 및 기본 정보 조회
     */
    @Operation(summary = "서비스 정보 조회", 
               description = "서비스 버전, 지원 통화, 주요 기능 등 기본 정보를 조회합니다.")
    @GetMapping("/service/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        return ResponseEntity.ok(Map.of(
            "serviceName", "HwanYulMate",
            "version", "1.0.0",
            "description", "실시간 환율 조회 및 환전 예상 금액 비교 서비스",
            "supportedCurrencies", Map.of(
                "count", 12,
                "list", new String[]{"USD", "EUR", "JPY", "CNY", "GBP", "CHF", "CAD", "AUD", "HKD", "THB", "SGD", "SEK"}
            ),
            "supportedBanks", Map.of(
                "count", 7,
                "list", new String[]{"우리은행", "신한은행", "국민은행", "하나은행", "농협은행", "기업은행", "외환은행"}
            ),
            "features", Map.of(
                "realTimeExchange", "실시간 환율 조회",
                "chartAnalysis", "평일 기준 차트 분석 (7일~1년)",
                "bankComparison", "은행별 환전 수수료 비교",
                "priceAlert", "목표 환율 알림",
                "newsSearch", "환율 관련 뉴스 검색"
            ),
            "dataSource", "한국수출입은행 공식 API",
            "updateSchedule", "평일 오전 9:30, 오후 3:00"
        ));
    }
}