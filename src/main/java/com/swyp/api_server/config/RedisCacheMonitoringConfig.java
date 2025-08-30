package com.swyp.api_server.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;

/**
 * Redis 캐시 모니터링 설정
 * - 캐시 기본 정보 모니터링
 * - 캐시 개수 및 상태 추적
 */
@Configuration
@Log4j2
@RequiredArgsConstructor
public class RedisCacheMonitoringConfig {
    
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    
    /**
     * 애플리케이션 시작 후 캐시 기본 메트릭 등록
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerCacheMetrics() {
        if (cacheManager instanceof RedisCacheManager) {
            RedisCacheManager redisCacheManager = (RedisCacheManager) cacheManager;
            
            log.info("Redis 캐시 모니터링 시작...");
            
            // 캐시 개수 게이지 등록
            meterRegistry.gauge("cache.count", 
                    Tags.of("type", "redis"), 
                    redisCacheManager.getCacheNames().size());
            
            // 각 캐시별 기본 정보 등록
            redisCacheManager.getCacheNames().forEach(cacheName -> {
                try {
                    Cache cache = redisCacheManager.getCache(cacheName);
                    if (cache instanceof RedisCache) {
                        // 캐시 상태 게이지 등록 (존재하면 1, 없으면 0)
                        meterRegistry.gauge("cache.status", 
                                Tags.of("cache", cacheName, "type", "redis"), 
                                1);
                        
                        log.info("캐시 모니터링 등록 완료: {}", cacheName);
                    }
                } catch (Exception e) {
                    log.warn("캐시 모니터링 등록 실패: {}, 오류: {}", cacheName, e.getMessage());
                }
            });
            
            log.info("Redis 캐시 모니터링 등록 완료: {} 개 캐시", redisCacheManager.getCacheNames().size());
        } else {
            log.warn("CacheManager가 RedisCacheManager가 아닙니다: {}", cacheManager.getClass().getSimpleName());
        }
    }
}