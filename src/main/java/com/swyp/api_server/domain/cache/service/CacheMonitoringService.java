package com.swyp.api_server.domain.cache.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 캐시 모니터링 서비스
 * - 캐시 적중률 추적 및 로깅
 * - Redis 메모리 사용량 모니터링
 * - 캐시 성능 통계 제공
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class CacheMonitoringService {
    
    private final CacheManager cacheManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    
    // 캐시별 히트/미스 통계 저장
    private final Map<String, CacheStats> cacheStatsMap = new HashMap<>();
    
    /**
     * 캐시 통계 주기적 로깅 (매 10분)
     */
    @Scheduled(fixedRate = 600000)
    public void logCacheStatistics() {
        log.info("=== Redis 캐시 통계 ===");
        
        // Redis 정보 조회
        logRedisInfo();
        
        // 캐시별 통계 조회
        cacheManager.getCacheNames().forEach(this::logCacheStats);
        
        // 전체 캐시 요약
        logCacheSummary();
    }
    
    /**
     * Redis 서버 정보 로깅
     */
    private void logRedisInfo() {
        try {
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            
            String usedMemory = info.getProperty("used_memory_human", "N/A");
            String totalConnections = info.getProperty("total_connections_received", "N/A");
            String connectedClients = info.getProperty("connected_clients", "N/A");
            String keyspaceHits = info.getProperty("keyspace_hits", "0");
            String keyspaceMisses = info.getProperty("keyspace_misses", "0");
            
            log.info("📊 Redis 서버 상태:");
            log.info("  • 메모리 사용량: {}", usedMemory);
            log.info("  • 연결된 클라이언트: {}", connectedClients);
            log.info("  • 총 연결 수: {}", totalConnections);
            
            // Redis 레벨 히트율 계산
            long hits = Long.parseLong(keyspaceHits);
            long misses = Long.parseLong(keyspaceMisses);
            double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;
            log.info("  • Redis 전체 히트율: {:.1f}% (히트: {}, 미스: {})", hitRate, hits, misses);
            
        } catch (Exception e) {
            log.warn("Redis 정보 조회 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 개별 캐시 통계 로깅
     */
    private void logCacheStats(String cacheName) {
        try {
            // Micrometer에서 캐시 메트릭 조회
            Timer.Sample sample = Timer.start(meterRegistry);
            
            // 캐시별 키 개수 조회
            Set<String> keys = redisTemplate.keys(cacheName + ":*");
            long keyCount = keys != null ? keys.size() : 0;
            
            // 캐시 TTL 정보 (샘플링)
            long avgTtl = 0;
            if (keys != null && !keys.isEmpty()) {
                avgTtl = keys.stream()
                        .limit(10) // 최대 10개만 샘플링
                        .mapToLong(key -> {
                            Long ttl = redisTemplate.getExpire(key);
                            return ttl != null ? ttl : 0;
                        })
                        .filter(ttl -> ttl > 0)
                        .sum() / Math.min(keys.size(), 10);
            }
            
            log.info("📈 캐시 '{}': 키 개수={}, 평균 TTL={}초", 
                    cacheName, keyCount, avgTtl);
            
            sample.stop(Timer.builder("cache.operation.duration")
                    .tag("cache", cacheName)
                    .register(meterRegistry));
            
        } catch (Exception e) {
            log.warn("캐시 '{}' 통계 조회 실패: {}", cacheName, e.getMessage());
        }
    }
    
    /**
     * 전체 캐시 요약 로깅
     */
    private void logCacheSummary() {
        try {
            Set<String> allKeys = redisTemplate.keys("*");
            long totalKeys = allKeys != null ? allKeys.size() : 0;
            
            log.info("📋 캐시 요약:");
            log.info("  • 총 캐시 수: {}", cacheManager.getCacheNames().size());
            log.info("  • 총 키 개수: {}", totalKeys);
            
            // 메모리 부족 경고
            if (totalKeys > 100000) {
                log.warn("⚠️ 캐시 키가 너무 많습니다 ({}개). 메모리 사용량을 확인하세요.", totalKeys);
            }
            
        } catch (Exception e) {
            log.warn("캐시 요약 통계 조회 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 캐시 성능 진단
     */
    public Map<String, Object> diagnoseCache() {
        Map<String, Object> diagnosis = new HashMap<>();
        
        try {
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            
            // 메모리 사용률
            String usedMemory = info.getProperty("used_memory", "0");
            String maxMemory = info.getProperty("maxmemory", "0");
            
            long used = Long.parseLong(usedMemory);
            long max = Long.parseLong(maxMemory);
            double memoryUsageRate = max > 0 ? (double) used / max * 100 : 0;
            
            diagnosis.put("memoryUsageRate", memoryUsageRate);
            diagnosis.put("usedMemoryHuman", info.getProperty("used_memory_human", "N/A"));
            
            // 연결 상태
            diagnosis.put("connectedClients", Long.parseLong(info.getProperty("connected_clients", "0")));
            diagnosis.put("blockedClients", Long.parseLong(info.getProperty("blocked_clients", "0")));
            
            // 키스페이스 통계
            long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
            long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
            double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;
            
            diagnosis.put("hitRate", hitRate);
            diagnosis.put("totalOperations", hits + misses);
            
            // 건강도 판정
            List<String> issues = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();
            
            if (memoryUsageRate > 80) {
                issues.add("메모리 사용률이 높습니다 (" + String.format("%.1f", memoryUsageRate) + "%)");
                recommendations.add("불필요한 캐시 키를 정리하거나 TTL을 단축하세요");
            }
            
            if (hitRate < 70) {
                issues.add("캐시 히트율이 낮습니다 (" + String.format("%.1f", hitRate) + "%)");
                recommendations.add("캐시 전략을 재검토하고 TTL을 조정하세요");
            }
            
            long connectedClients = Long.parseLong(info.getProperty("connected_clients", "0"));
            if (connectedClients > 50) {
                issues.add("연결된 클라이언트가 많습니다 (" + connectedClients + "개)");
                recommendations.add("연결 풀 설정을 확인하세요");
            }
            
            diagnosis.put("issues", issues);
            diagnosis.put("recommendations", recommendations);
            diagnosis.put("overallHealth", issues.isEmpty() ? "GOOD" : "WARNING");
            
        } catch (Exception e) {
            diagnosis.put("error", "Redis 진단 중 오류: " + e.getMessage());
            diagnosis.put("overallHealth", "ERROR");
        }
        
        return diagnosis;
    }
    
    /**
     * 캐시 성능 최적화 제안
     */
    public List<String> getOptimizationSuggestions() {
        List<String> suggestions = new ArrayList<>();
        
        try {
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            
            // 메모리 기반 제안
            String usedMemory = info.getProperty("used_memory", "0");
            long used = Long.parseLong(usedMemory);
            
            if (used > 1024 * 1024 * 100) { // 100MB 이상
                suggestions.add("💾 메모리 사용량이 높습니다. 캐시 TTL 단축을 고려하세요");
            }
            
            // 히트율 기반 제안
            long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
            long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
            double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;
            
            if (hitRate < 50) {
                suggestions.add("📉 캐시 히트율이 낮습니다. 캐시 키 패턴을 재검토하세요");
            } else if (hitRate > 95) {
                suggestions.add("📈 캐시 히트율이 높습니다. TTL을 늘려서 더 오래 캐시할 수 있습니다");
            }
            
            // 키 개수 기반 제안
            Set<String> allKeys = redisTemplate.keys("*");
            long totalKeys = allKeys != null ? allKeys.size() : 0;
            
            if (totalKeys > 50000) {
                suggestions.add("🔑 캐시 키가 많습니다. 정기적인 정리 작업을 고려하세요");
            }
            
            // 연결 기반 제안
            long clients = Long.parseLong(info.getProperty("connected_clients", "0"));
            if (clients < 2) {
                suggestions.add("🔌 연결된 클라이언트가 적습니다. 연결 풀 min-idle 설정을 확인하세요");
            }
            
        } catch (Exception e) {
            suggestions.add("❌ 최적화 제안 생성 중 오류: " + e.getMessage());
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("✅ 현재 Redis 캐시 성능이 양호합니다");
        }
        
        return suggestions;
    }
    
    /**
     * 캐시별 상세 통계 조회
     */
    public Map<String, Object> getCacheDetailStats() {
        Map<String, Object> detailStats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            try {
                Map<String, Object> cacheInfo = new HashMap<>();
                
                // 키 개수
                Set<String> keys = redisTemplate.keys(cacheName + ":*");
                cacheInfo.put("keyCount", keys != null ? keys.size() : 0);
                
                // TTL 통계
                if (keys != null && !keys.isEmpty()) {
                    OptionalDouble avgTtl = keys.stream()
                            .limit(100) // 최대 100개 샘플링
                            .mapToLong(key -> {
                                Long ttl = redisTemplate.getExpire(key);
                                return ttl != null && ttl > 0 ? ttl : 0;
                            })
                            .filter(ttl -> ttl > 0)
                            .average();
                    
                    cacheInfo.put("averageTTL", avgTtl.orElse(0));
                }
                
                detailStats.put(cacheName, cacheInfo);
                
            } catch (Exception e) {
                Map<String, Object> errorInfo = new HashMap<>();
                errorInfo.put("error", e.getMessage());
                detailStats.put(cacheName, errorInfo);
            }
        });
        
        return detailStats;
    }
    
    /**
     * 캐시 통계 내부 클래스
     */
    public static class CacheStats {
        public long hits = 0;
        public long misses = 0;
        public long evictions = 0;
        
        public double getHitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total * 100 : 0;
        }
    }
}