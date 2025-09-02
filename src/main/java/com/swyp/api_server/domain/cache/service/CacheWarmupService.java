package com.swyp.api_server.domain.cache.service;

import com.swyp.api_server.domain.rate.service.ExchangeRateService;
import com.swyp.api_server.domain.rate.service.NewsService;
import com.swyp.api_server.domain.rate.service.BankExchangeInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 캐시 워밍업 서비스
 * - 서버 시작 시 주요 데이터 미리 캐싱
 * - 주기적 캐시 갱신으로 사용자 응답 속도 향상
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class CacheWarmupService {
    
    private final ExchangeRateService exchangeRateService;
    private final NewsService newsService;
    private final BankExchangeInfoService bankExchangeInfoService;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${cache.warmup.enabled:true}")
    private boolean warmupEnabled;
    
    @Value("${cache.warmup.fail-fast:false}")
    private boolean failFast;
    
    // 주요 통화 목록 (많이 조회되는 통화들)
    private static final List<String> MAJOR_CURRENCIES = Arrays.asList(
            "USD", "EUR", "JPY", "GBP", "CHF", "CAD", "AUD", "CNH"
    );
    
    /**
     * 애플리케이션 시작 시 캐시 워밍업 (비활성화됨)
     */
    // @EventListener(ApplicationReadyEvent.class)
    // @Async
    public void warmupCacheOnStartup() {
        if (!warmupEnabled) {
            log.info("🔥 캐시 워밍업이 비활성화되어 있습니다. (cache.warmup.enabled=false)");
            return;
        }
        
        log.info("🔥 캐시 워밍업 시작...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 병렬로 캐시 워밍업 실행
            CompletableFuture<Void> exchangeRatesWarmup = warmupExchangeRates();
            CompletableFuture<Void> newsWarmup = warmupNews();
            CompletableFuture<Void> bankInfoWarmup = warmupBankInfo();
            
            // 모든 워밍업 작업 완료 대기
            CompletableFuture.allOf(exchangeRatesWarmup, newsWarmup, bankInfoWarmup)
                    .join();
            
            long endTime = System.currentTimeMillis();
            log.info("✅ 캐시 워밍업 완료: {}ms 소요", endTime - startTime);
            
            // 워밍업 결과 통계 로깅
            logWarmupStatistics();
            
        } catch (Exception e) {
            if (failFast) {
                log.error("❌ 캐시 워밍업 중 오류 발생 (fail-fast=true): {}", e.getMessage(), e);
                throw new RuntimeException("캐시 워밍업 실패로 인한 애플리케이션 시작 중단", e);
            } else {
                log.warn("⚠️ 캐시 워밍업 중 오류 발생했지만 애플리케이션은 계속 실행합니다: {}", e.getMessage());
                log.debug("캐시 워밍업 오류 상세:", e);
            }
        }
    }
    
    /**
     * 환율 데이터 캐시 워밍업
     */
    @Async
    public CompletableFuture<Void> warmupExchangeRates() {
        try {
            log.info("환율 데이터 캐시 워밍업 시작...");
            
            // 전체 환율 목록 캐싱
            exchangeRateService.getAllExchangeRates();
            log.debug("전체 환율 목록 캐싱 완료");
            
            // 주요 통화별 실시간 환율 캐싱
            for (String currency : MAJOR_CURRENCIES) {
                try {
                    exchangeRateService.getRealtimeExchangeRate(currency);
                    log.debug("실시간 환율 캐싱 완료: {}", currency);
                } catch (Exception e) {
                    log.warn("실시간 환율 캐싱 실패: {}, 오류: {}", currency, e.getMessage());
                }
            }
            
            // 주요 통화별 과거 환율 데이터 캐싱 (7일, 30일)
            for (String currency : MAJOR_CURRENCIES) {
                try {
                    exchangeRateService.getHistoricalExchangeRate(currency, 7);
                    exchangeRateService.getHistoricalExchangeRate(currency, 30);
                    log.debug("과거 환율 데이터 캐싱 완료: {}", currency);
                } catch (Exception e) {
                    log.warn("과거 환율 데이터 캐싱 실패: {}, 오류: {}", currency, e.getMessage());
                }
            }
            
            log.info("환율 데이터 캐시 워밍업 완료");
            
        } catch (Exception e) {
            log.warn("환율 데이터 캐시 워밍업 중 오류 (외부 API 연동 문제 가능성): {}", e.getMessage());
            log.debug("환율 데이터 캐시 워밍업 오류 상세:", e);
            // 환율 API 오류는 흔하므로 애플리케이션 실행을 중단하지 않음
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 뉴스 데이터 캐시 워밍업
     */
    @Async
    public CompletableFuture<Void> warmupNews() {
        try {
            log.info("뉴스 데이터 캐시 워밍업 시작...");
            
            // 전체 환율 뉴스 캐싱
            newsService.getExchangeNews();
            log.debug("환율 뉴스 캐싱 완료");
            
            // 주요 통화별 뉴스 캐싱
            for (String currency : MAJOR_CURRENCIES) {
                try {
                    newsService.getCurrencyNews(currency);
                    log.debug("통화별 뉴스 캐싱 완료: {}", currency);
                } catch (Exception e) {
                    log.warn("통화별 뉴스 캐싱 실패: {}, 오류: {}", currency, e.getMessage());
                }
            }
            
            log.info("뉴스 데이터 캐시 워밍업 완료");
            
        } catch (Exception e) {
            log.error("뉴스 데이터 캐시 워밍업 중 오류: {}", e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 은행 정보 캐시 워밍업
     */
    @Async
    public CompletableFuture<Void> warmupBankInfo() {
        try {
            log.info("은행 정보 캐시 워밍업 시작...");
            
            // 모든 활성 은행 정보 캐싱
            bankExchangeInfoService.getAllActiveBanks();
            log.debug("은행 정보 캐싱 완료");
            
            // 온라인 환전 가능 은행 정보 캐싱
            bankExchangeInfoService.getOnlineAvailableBanks();
            log.debug("온라인 환전 은행 정보 캐싱 완료");
            
            log.info("은행 정보 캐시 워밍업 완료");
            
        } catch (Exception e) {
            log.error("은행 정보 캐시 워밍업 중 오류: {}", e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 주기적 캐시 재워밍업 (매일 새벽 1시)
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void scheduledCacheWarmup() {
        if (!warmupEnabled) {
            log.debug("캐시 워밍업이 비활성화되어 있어 주기적 워밍업을 건너뜁니다.");
            return;
        }
        log.info("주기적 캐시 워밍업 시작...");
        warmupCacheOnStartup();
    }
    
    /**
     * 캐시 사용률이 낮은 시간대 워밍업 (오전 6시)
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void morningCacheWarmup() {
        if (!warmupEnabled) {
            log.debug("캐시 워밍업이 비활성화되어 있어 오전 워밍업을 건너뜁니다.");
            return;
        }
        log.info("오전 캐시 워밍업 시작...");
        
        // 오전에는 환율 데이터와 뉴스만 워밍업
        CompletableFuture<Void> exchangeWarmup = warmupExchangeRates();
        CompletableFuture<Void> newsWarmup = warmupNews();
        
        CompletableFuture.allOf(exchangeWarmup, newsWarmup).join();
        
        log.info("오전 캐시 워밍업 완료");
    }
    
    /**
     * 워밍업 통계 로깅
     */
    private void logWarmupStatistics() {
        try {
            java.util.Set<String> allKeys = redisTemplate.keys("*");
            long totalKeys = allKeys != null ? allKeys.size() : 0;
            
            // 캐시별 키 개수 통계
            java.util.Map<String, Long> cacheStats = new java.util.HashMap<>();
            
            if (allKeys != null) {
                for (String key : allKeys) {
                    String cacheType = key.contains(":") ? key.split(":")[0] : "unknown";
                    cacheStats.merge(cacheType, 1L, Long::sum);
                }
            }
            
            log.info("📊 캐시 워밍업 통계:");
            log.info("  • 총 캐시 키: {} 개", totalKeys);
            
            cacheStats.forEach((cacheType, count) -> 
                    log.info("  • {}: {} 개", cacheType, count));
            
        } catch (Exception e) {
            log.warn("워밍업 통계 조회 중 오류: {}", e.getMessage());
        }
    }
    
    /**
     * 특정 통화의 캐시 선제적 갱신
     */
    public void preemptiveWarmup(String currencyCode) {
        try {
            log.info("특정 통화 선제적 워밍업 시작: {}", currencyCode);
            
            // 실시간 환율
            exchangeRateService.getRealtimeExchangeRate(currencyCode);
            
            // 과거 환율 (일주일)
            exchangeRateService.getHistoricalExchangeRate(currencyCode, 7);
            
            // 해당 통화 뉴스
            newsService.getCurrencyNews(currencyCode);
            
            log.info("특정 통화 선제적 워밍업 완료: {}", currencyCode);
            
        } catch (Exception e) {
            log.error("특정 통화 선제적 워밍업 실패: {}, 오류: {}", currencyCode, e.getMessage());
        }
    }
    
    /**
     * 캐시 워밍업 상태 체크
     */
    public java.util.Map<String, Object> getWarmupStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        
        try {
            java.util.Set<String> allKeys = redisTemplate.keys("*");
            long totalKeys = allKeys != null ? allKeys.size() : 0;
            
            // 캐시 타입별 통계
            java.util.Map<String, Long> cacheTypeStats = new java.util.HashMap<>();
            if (allKeys != null) {
                for (String key : allKeys) {
                    String cacheType = key.contains(":") ? key.split(":")[0] : "unknown";
                    cacheTypeStats.merge(cacheType, 1L, Long::sum);
                }
            }
            
            status.put("totalCacheKeys", totalKeys);
            status.put("cacheTypeStats", cacheTypeStats);
            
            // 주요 통화별 캐시 존재 여부 체크
            java.util.Map<String, Boolean> currencyCacheStatus = new java.util.HashMap<>();
            for (String currency : MAJOR_CURRENCIES) {
                boolean hasRealtimeCache = redisTemplate.hasKey("realtimeRate:" + currency);
                currencyCacheStatus.put(currency, hasRealtimeCache);
            }
            
            status.put("majorCurrenciesCache", currencyCacheStatus);
            status.put("warmupComplete", totalKeys > 50); // 50개 이상의 키가 있으면 워밍업 완료로 간주
            
        } catch (Exception e) {
            status.put("error", "워밍업 상태 확인 중 오류: " + e.getMessage());
            status.put("warmupComplete", false);
        }
        
        return status;
    }
    
    /**
     * 캐시 강제 워밍업 (관리자용)
     */
    public void forceWarmup() {
        log.info("관리자 요청 캐시 강제 워밍업 시작");
        warmupCacheOnStartup();
    }
}