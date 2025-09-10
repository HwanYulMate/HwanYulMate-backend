package com.swyp.api_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.api_server.common.constants.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Redis 기반 캐시 매니저 설정
     * @param redisConnectionFactory Redis 연결 팩토리
     * @return RedisCacheManager 인스턴스
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        
        // 타입 정보 포함하는 완전한 ObjectMapper 생성 (Entity 직렬화용)
        ObjectMapper cacheMapper = new ObjectMapper();
        cacheMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        cacheMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Entity 직렬화를 위한 타입 정보 활성화
        cacheMapper.activateDefaultTyping(
            com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.swyp.api_server.entity")
                .allowIfSubType("com.swyp.api_server.domain")
                .allowIfSubType("java.util")
                .allowIfSubType("java.lang")
                .allowIfSubType("java.math")
                .allowIfSubType("java.time")
                .build(),
            com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        // GenericJackson2JsonRedisSerializer 사용 (타입 정보 포함하지 않음)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(cacheMapper);
        
        // 기본 캐시 설정
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(Constants.Cache.DEFAULT_TTL_MINUTES))  // 기본 TTL
                .disableCachingNullValues()       // null 값 캐싱 비활성화
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
        
        // 캐시별 개별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 전체 환율 목록: 5분 캐시 (수출입은행 API 제한 고려 - 1000회/일)
        cacheConfigurations.put(Constants.Cache.EXCHANGE_RATES, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(Constants.Cache.EXCHANGE_RATE_TTL_MINUTES)));
        
        // 실시간 환율: 5분 캐시 (수출입은행 API 제한 고려)
        cacheConfigurations.put(Constants.Cache.REALTIME_RATE, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(Constants.Cache.REALTIME_RATE_TTL_MINUTES)));
        
        // 과거 환율 데이터: 10분 캐시 (변동이 적으므로 오래 캐싱)
        cacheConfigurations.put(Constants.Cache.HISTORICAL_RATE, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(Constants.Cache.HISTORICAL_RATE_TTL_MINUTES)));
        
        // 뉴스 데이터: 30분 캐시 (뉴스는 실시간성이 상대적으로 덜 중요)
        cacheConfigurations.put(Constants.Cache.EXCHANGE_NEWS, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(Constants.Cache.NEWS_TTL_MINUTES)));
        cacheConfigurations.put(Constants.Cache.CURRENCY_NEWS, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(Constants.Cache.NEWS_TTL_MINUTES)));
        cacheConfigurations.put(Constants.Cache.NEWS, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(Constants.Cache.NEWS_TTL_MINUTES)));
        
        // 환전 계산 데이터: 2분 캐시 (실시간 환율 반영하되 계산 부하 줄임)
        cacheConfigurations.put(Constants.Cache.EXCHANGE_CALCULATION, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(Constants.Cache.EXCHANGE_CALCULATION_TTL_MINUTES)));
        
        // 은행 정보 데이터: 30분 캐시 (자주 변경되지 않는 설정 정보)
        cacheConfigurations.put(Constants.Cache.BANK_EXCHANGE_INFO, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(Constants.Cache.BANK_INFO_TTL_MINUTES)));
        
        // FCM 관련 캐시: 짧은 TTL로 실시간성 보장
        cacheConfigurations.put(Constants.Cache.FCM_DUPLICATE, 
            defaultCacheConfig.entryTtl(Duration.ofHours(Constants.Cache.FCM_DUPLICATE_TTL_HOURS))); // 중복 방지는 하루
        cacheConfigurations.put(Constants.Cache.FCM_FAILED_TOKENS, 
            defaultCacheConfig.entryTtl(Duration.ofHours(Constants.Cache.FCM_FAILED_TOKENS_TTL_HOURS))); // 실패 토큰은 6시간
        
        // 분산 락 캐시: 매우 짧은 TTL
        cacheConfigurations.put(Constants.Cache.DISTRIBUTED_LOCK, 
            defaultCacheConfig.entryTtl(Duration.ofMinutes(Constants.Cache.DISTRIBUTED_LOCK_TTL_MINUTES)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
    
    /**
     * RedisTemplate 설정 - GenericJackson2JsonRedisSerializer 사용
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // 타입 정보 없는 안전한 ObjectMapper 생성
        ObjectMapper cacheMapper = new ObjectMapper();
        cacheMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        cacheMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(cacheMapper);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}