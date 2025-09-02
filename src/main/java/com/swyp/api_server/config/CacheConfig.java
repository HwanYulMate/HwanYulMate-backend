package com.swyp.api_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
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
        
        // 타입 정보 없는 안전한 ObjectMapper 생성
        ObjectMapper cacheMapper = new ObjectMapper();
        cacheMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        cacheMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // GenericJackson2JsonRedisSerializer 사용 (타입 정보 포함하지 않음)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(cacheMapper);
        
        // 기본 캐시 설정
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // 기본 5분 TTL
                .disableCachingNullValues()       // null 값 캐싱 비활성화
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
        
        // 캐시별 개별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 전체 환율 목록: 5분 캐시 (수출입은행 API 제한 고려 - 1000회/일)
        cacheConfigurations.put("exchangeRates", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 실시간 환율: 5분 캐시 (수출입은행 API 제한 고려)
        cacheConfigurations.put("realtimeRate", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 과거 환율 데이터: 10분 캐시 (변동이 적으므로 오래 캐싱)
        cacheConfigurations.put("historicalRate", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 뉴스 데이터: 30분 캐시 (뉴스는 실시간성이 상대적으로 덜 중요)
        cacheConfigurations.put("exchangeNews", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("currencyNews", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("news", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 환전 계산 데이터: 2분 캐시 (실시간 환율 반영하되 계산 부하 줄임)
        cacheConfigurations.put("exchangeCalculation", defaultCacheConfig.entryTtl(Duration.ofMinutes(2)));
        
        // 은행 정보 데이터: 30분 캐시 (자주 변경되지 않는 설정 정보)
        cacheConfigurations.put("bankExchangeInfo", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        
        // FCM 관련 캐시: 짧은 TTL로 실시간성 보장
        cacheConfigurations.put("fcmDuplicate", defaultCacheConfig.entryTtl(Duration.ofHours(24))); // 중복 방지는 하루
        cacheConfigurations.put("fcmFailedTokens", defaultCacheConfig.entryTtl(Duration.ofHours(6))); // 실패 토큰은 6시간
        
        // 분산 락 캐시: 매우 짧은 TTL
        cacheConfigurations.put("distributedLock", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        
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