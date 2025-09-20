package com.swyp.api_server.domain.rate.controller;

import com.swyp.api_server.domain.rate.service.ExchangeRateHistoryInitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 환율 데이터 초기화 컨트롤러
 * - 관리자용 초기 데이터 로딩 API
 * - 운영 환경에서는 비활성화 권장
 */
@Tag(name = "환율 데이터 초기화", description = "환율 히스토리 데이터 초기화 API (관리자용)")
@RestController
@RequestMapping("/api/admin/exchange")
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateInitController {

    private final ExchangeRateHistoryInitService initService;

    /**
     * 1년치 환율 히스토리 데이터 초기화
     * - 과거 1년간 평일 환율 데이터를 일괄 수집
     * - 서비스 초기 구동 시 사용
     */
    @Operation(summary = "환율 히스토리 초기화", 
               description = "과거 1년치 평일 환율 데이터를 수집하여 히스토리 테이블에 저장합니다. " +
                           "기존 데이터가 있으면 건너뜁니다.")
    @PostMapping("/init-history")
    public ResponseEntity<Map<String, Object>> initializeHistoricalData() {
        try {
            log.info("환율 히스토리 초기화 요청 받음");
            
            // 초기화 필요 여부 확인
            if (!initService.needsInitialization()) {
                log.info("이미 히스토리 데이터가 존재하여 초기화를 건너뜀");
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이미 히스토리 데이터가 존재합니다.",
                    "action", "skipped"
                ));
            }

            // 초기화 실행
            initService.initializeHistoricalData();
            
            log.info("환율 히스토리 초기화 완료");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "1년치 환율 히스토리 데이터 초기화가 완료되었습니다.",
                "action", "completed"
            ));
            
        } catch (Exception e) {
            log.error("환율 히스토리 초기화 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "환율 히스토리 초기화 중 오류가 발생했습니다: " + e.getMessage(),
                "action", "failed"
            ));
        }
    }

    /**
     * 강제 재초기화 (기존 데이터 삭제 후 재생성)
     * - 주의: 기존 히스토리 데이터가 모두 삭제됩니다
     */
    @Operation(summary = "환율 히스토리 강제 재초기화", 
               description = "기존 히스토리 데이터를 삭제하고 1년치 데이터를 재수집합니다. " +
                           "주의: 기존 데이터가 모두 삭제됩니다.")
    @PostMapping("/force-reinit-history")
    public ResponseEntity<Map<String, Object>> forceReinitializeHistoricalData() {
        try {
            log.warn("환율 히스토리 강제 재초기화 요청 받음");
            
            initService.forceReinitialize();
            
            log.info("환율 히스토리 강제 재초기화 완료");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "환율 히스토리 데이터 강제 재초기화가 완료되었습니다.",
                "action", "force_completed"
            ));
            
        } catch (Exception e) {
            log.error("환율 히스토리 강제 재초기화 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "환율 히스토리 강제 재초기화 중 오류가 발생했습니다: " + e.getMessage(),
                "action", "failed"
            ));
        }
    }

    /**
     * 히스토리 데이터 확장 (3개월, 6개월, 1년)
     */
    @Operation(summary = "환율 히스토리 확장", 
               description = "기존 데이터를 3개월/6개월/1년으로 확장합니다. (API 호출 최적화)")
    @PostMapping("/expand-history/{days}")
    public ResponseEntity<Map<String, Object>> expandHistoricalData(@PathVariable int days) {
        try {
            if (days != 90 && days != 180 && days != 365) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "지원되는 확장 기간: 90일(3개월), 180일(6개월), 365일(1년)"
                ));
            }

            log.info("환율 히스토리 확장 요청: {} 일", days);
            initService.expandHistoricalData(days);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", days + "일치 환율 히스토리 확장이 완료되었습니다.",
                "expandedDays", days
            ));
            
        } catch (Exception e) {
            log.error("환율 히스토리 확장 실패: {} 일", days, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "환율 히스토리 확장 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 초기화 상태 확인
     */
    @Operation(summary = "초기화 상태 확인", 
               description = "환율 히스토리 데이터 초기화가 필요한지 확인합니다.")
    @GetMapping("/init-status")
    public ResponseEntity<Map<String, Object>> checkInitializationStatus() {
        try {
            boolean needsInit = initService.needsInitialization();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "needsInitialization", needsInit,
                "message", needsInit ? "초기화가 필요합니다." : "이미 초기화되어 있습니다."
            ));
            
        } catch (Exception e) {
            log.error("초기화 상태 확인 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "초기화 상태 확인 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}