package com.swyp.api_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 * - 외부 API 호출을 위한 스레드 풀 구성
 * - 트랜잭션과 외부 의존성 분리
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 외부 API 호출용 스레드 풀
     * - Apple 토큰 검증, 토큰 무효화 등
     */
    @Bean("externalApiExecutor")
    public Executor externalApiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ExternalAPI-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        
        log.info("외부 API 호출용 스레드 풀 초기화 완료: 코어={}, 최대={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize());
        
        return executor;
    }
}