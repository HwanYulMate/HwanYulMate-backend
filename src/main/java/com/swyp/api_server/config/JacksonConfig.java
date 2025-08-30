package com.swyp.api_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson JSON 직렬화 설정
 * - Java 8 시간 타입 (LocalDateTime, LocalDate 등) 지원
 * - Redis 캐싱 시 LocalDateTime 직렬화 오류 해결
 */
@Configuration
public class JacksonConfig {

    /**
     * ObjectMapper 빈 설정
     * @return JSR310 모듈이 포함된 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java 8 시간 모듈 등록
        mapper.registerModule(new JavaTimeModule());
        
        // 날짜를 타임스탬프가 아닌 ISO-8601 형식으로 직렬화
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}