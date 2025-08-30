package com.swyp.api_server.domain.cache.controller;

import com.swyp.api_server.domain.cache.service.CacheMonitoringService;
import com.swyp.api_server.domain.cache.service.CacheWarmupService;
import com.swyp.api_server.domain.notification.service.FCMDuplicatePreventionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 캐시 관리 및 모니터링 컨트롤러
 * - Redis 캐시 상태 조회
 * - 캐시 워밍업 제어
 * - 캐시 통계 및 진단
 */
@RestController
@RequestMapping("/api/cache/admin")
@RequiredArgsConstructor
@Tag(name = "캐시 관리", description = "Redis 캐시 관리 및 모니터링 API")
public class CacheController {
    
    private final CacheManager cacheManager;
    private final CacheMonitoringService cacheMonitoringService;
    private final CacheWarmupService cacheWarmupService;
    private final FCMDuplicatePreventionService fcmDuplicatePreventionService;
    
    /**
     * 캐시 전체 통계 조회
     */
    @GetMapping("/statistics")
    @Operation(summary = "캐시 통계 조회", description = "Redis 캐시 전체 통계를 조회합니다")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> stats = cacheMonitoringService.getCacheDetailStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 캐시 성능 진단
     */
    @GetMapping("/diagnosis")
    @Operation(summary = "캐시 성능 진단", description = "Redis 캐시 성능을 진단하고 문제점을 분석합니다")
    public ResponseEntity<Map<String, Object>> diagnoseCachePerformance() {
        Map<String, Object> diagnosis = cacheMonitoringService.diagnoseCache();
        return ResponseEntity.ok(diagnosis);
    }
    
    /**
     * 캐시 최적화 제안
     */
    @GetMapping("/optimization-suggestions")
    @Operation(summary = "캐시 최적화 제안", description = "캐시 성능 최적화 제안사항을 제공합니다")
    public ResponseEntity<java.util.List<String>> getOptimizationSuggestions() {
        java.util.List<String> suggestions = cacheMonitoringService.getOptimizationSuggestions();
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * 캐시 워밍업 상태 조회
     */
    @GetMapping("/warmup/status")
    @Operation(summary = "캐시 워밍업 상태", description = "캐시 워밍업 완료 상태를 조회합니다")
    public ResponseEntity<Map<String, Object>> getWarmupStatus() {
        Map<String, Object> status = cacheWarmupService.getWarmupStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * 캐시 강제 워밍업 실행
     */
    @PostMapping("/warmup/force")
    @Operation(summary = "캐시 강제 워밍업", description = "모든 캐시를 강제로 워밍업합니다")
    public ResponseEntity<String> forceWarmup() {
        cacheWarmupService.forceWarmup();
        return ResponseEntity.ok("캐시 강제 워밍업이 시작되었습니다.");
    }
    
    /**
     * 특정 통화 캐시 선제적 워밍업
     */
    @PostMapping("/warmup/currency/{currencyCode}")
    @Operation(summary = "통화별 캐시 워밍업", description = "특정 통화의 캐시를 선제적으로 워밍업합니다")
    public ResponseEntity<String> warmupCurrency(@PathVariable String currencyCode) {
        cacheWarmupService.preemptiveWarmup(currencyCode.toUpperCase());
        return ResponseEntity.ok("통화 '" + currencyCode + "' 캐시 워밍업이 시작되었습니다.");
    }
    
    /**
     * 모든 캐시 이름 조회
     */
    @GetMapping("/names")
    @Operation(summary = "캐시 이름 목록", description = "설정된 모든 캐시 이름을 조회합니다")
    public ResponseEntity<java.util.Collection<String>> getCacheNames() {
        return ResponseEntity.ok(cacheManager.getCacheNames());
    }
    
    /**
     * 특정 캐시 삭제 (개발/테스트용)
     */
    @DeleteMapping("/{cacheName}")
    @Operation(summary = "캐시 삭제", description = "특정 캐시의 모든 데이터를 삭제합니다")
    public ResponseEntity<String> clearCache(@PathVariable String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return ResponseEntity.ok("캐시 '" + cacheName + "'가 삭제되었습니다.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 모든 캐시 삭제 (개발/테스트용)
     */
    @DeleteMapping("/all")
    @Operation(summary = "모든 캐시 삭제", description = "모든 캐시 데이터를 삭제합니다")
    public ResponseEntity<String> clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        return ResponseEntity.ok("모든 캐시가 삭제되었습니다.");
    }
    
    /**
     * FCM 중복 방지 캐시 통계
     */
    @GetMapping("/fcm-duplication/stats")
    @Operation(summary = "FCM 중복 방지 통계", description = "FCM 알림 중복 방지 캐시 통계를 조회합니다")
    public ResponseEntity<Map<String, Long>> getFCMDuplicationStats() {
        Map<String, Long> stats = fcmDuplicatePreventionService.getDuplicationStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * FCM 중복 방지 캐시 정리
     */
    @PostMapping("/fcm-duplication/cleanup")
    @Operation(summary = "FCM 중복 방지 캐시 정리", description = "만료된 FCM 중복 방지 캐시를 정리합니다")
    public ResponseEntity<String> cleanupFCMDuplicationCache() {
        fcmDuplicatePreventionService.cleanupExpiredCache();
        return ResponseEntity.ok("FCM 중복 방지 캐시 정리가 완료되었습니다.");
    }
    
    /**
     * FCM 토큰 차단 해제
     */
    @PostMapping("/fcm-duplication/unblock-token")
    @Operation(summary = "FCM 토큰 차단 해제", description = "차단된 FCM 토큰을 해제합니다")
    public ResponseEntity<String> unblockFCMToken(@RequestParam String fcmToken) {
        fcmDuplicatePreventionService.unblockToken(fcmToken);
        return ResponseEntity.ok("FCM 토큰 차단이 해제되었습니다: " + fcmToken);
    }
    
    /**
     * 캐시 건강 상태 체크
     */
    @GetMapping("/health")
    @Operation(summary = "캐시 건강 상태", description = "Redis 캐시 서비스의 건강 상태를 확인합니다")
    public ResponseEntity<Map<String, Object>> cacheHealthCheck() {
        Map<String, Object> diagnosis = cacheMonitoringService.diagnoseCache();
        String health = (String) diagnosis.get("overallHealth");
        
        Map<String, Object> healthStatus = Map.of(
                "healthy", "GOOD".equals(health),
                "status", health,
                "service", "Redis Cache",
                "details", diagnosis
        );
        
        return ResponseEntity.ok(healthStatus);
    }
}