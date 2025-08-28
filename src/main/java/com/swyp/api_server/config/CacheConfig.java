package com.swyp.api_server.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정 클래스
 * - 환율 데이터를 Redis에 캐싱하여 API 호출 최적화
 * - 캐시별 TTL 설정으로 세밀한 만료 시간 제어
 * - JSON 직렬화로 가독성 향상
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Redis 기반 캐시 매니저 설정
     * @param redisConnectionFactory Redis 연결 팩토리
     * @return RedisCacheManager 인스턴스
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        
        // 기본 캐시 설정
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // 기본 5분 TTL
                .disableCachingNullValues()       // null 값 캐싱 비활성화
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        // 캐시별 개별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 전체 환율 목록: 1분 캐시
        cacheConfigurations.put("exchangeRates", defaultCacheConfig.entryTtl(Duration.ofMinutes(1)));
        
        // 실시간 환율: 1분 캐시 
        cacheConfigurations.put("realtimeRate", defaultCacheConfig.entryTtl(Duration.ofMinutes(1)));
        
        // 과거 환율 데이터: 10분 캐시 (변동이 적으므로 오래 캐싱)
        cacheConfigurations.put("historicalRate", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 뉴스 데이터: 30분 캐시 (뉴스는 실시간성이 상대적으로 덜 중요)
        cacheConfigurations.put("exchangeNews", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("currencyNews", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}