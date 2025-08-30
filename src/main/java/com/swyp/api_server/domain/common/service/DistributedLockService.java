package com.swyp.api_server.domain.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 분산 락 서비스
 * - 스케줄러 중복 실행 방지
 * - 멀티 인스턴스 환경에서의 동시성 제어
 */
@Service
@Log4j2
public class DistributedLockService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String LOCK_PREFIX = "distributed_lock:";
    private static final String SCHEDULER_LOCK_PREFIX = "scheduler_lock:";
    
    // Lua 스크립트로 원자적 잠금 해제
    private static final String UNLOCK_SCRIPT = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";
    
    private final DefaultRedisScript<Long> unlockScript;
    
    public DistributedLockService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.unlockScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
    }
    
    /**
     * 분산 락 획득 시도
     * @param lockKey 락 키
     * @param lockValue 락 값 (고유 식별자)
     * @param expireTime 만료 시간 (초)
     * @return 락 획득 성공 여부
     */
    public boolean tryLock(String lockKey, String lockValue, long expireTime) {
        String key = LOCK_PREFIX + lockKey;
        
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, lockValue, Duration.ofSeconds(expireTime));
        
        boolean acquired = Boolean.TRUE.equals(result);
        
        if (acquired) {
            log.debug("분산 락 획득 성공: key={}, value={}, expire={}초", lockKey, lockValue, expireTime);
        } else {
            log.debug("분산 락 획득 실패: key={}, value={}", lockKey, lockValue);
        }
        
        return acquired;
    }
    
    /**
     * 분산 락 해제
     * @param lockKey 락 키
     * @param lockValue 락 값 (획득 시 사용한 값과 동일해야 함)
     * @return 락 해제 성공 여부
     */
    public boolean unlock(String lockKey, String lockValue) {
        String key = LOCK_PREFIX + lockKey;
        
        Long result = redisTemplate.execute(unlockScript, 
                Collections.singletonList(key), lockValue);
        
        boolean released = result != null && result > 0;
        
        if (released) {
            log.debug("분산 락 해제 성공: key={}, value={}", lockKey, lockValue);
        } else {
            log.debug("분산 락 해제 실패: key={}, value={} (이미 만료되었거나 다른 값)", lockKey, lockValue);
        }
        
        return released;
    }
    
    /**
     * 스케줄러 전용 락 획득 (짧은 만료 시간)
     * @param schedulerName 스케줄러 이름
     * @param instanceId 인스턴스 고유 ID
     * @return 락 획득 성공 여부
     */
    public boolean trySchedulerLock(String schedulerName, String instanceId) {
        String key = SCHEDULER_LOCK_PREFIX + schedulerName;
        
        // 스케줄러는 보통 짧은 주기로 실행되므로 5분 만료
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, instanceId, Duration.ofMinutes(5));
        
        boolean acquired = Boolean.TRUE.equals(result);
        
        if (acquired) {
            log.info("스케줄러 락 획득: scheduler={}, instance={}", schedulerName, instanceId);
        } else {
            String currentLockHolder = redisTemplate.opsForValue().get(key);
            log.debug("스케줄러 락 이미 점유됨: scheduler={}, holder={}, requester={}", 
                    schedulerName, currentLockHolder, instanceId);
        }
        
        return acquired;
    }
    
    /**
     * 스케줄러 락 해제
     * @param schedulerName 스케줄러 이름
     * @param instanceId 인스턴스 고유 ID
     * @return 락 해제 성공 여부
     */
    public boolean releaseSchedulerLock(String schedulerName, String instanceId) {
        String key = SCHEDULER_LOCK_PREFIX + schedulerName;
        
        Long result = redisTemplate.execute(unlockScript, 
                Collections.singletonList(key), instanceId);
        
        boolean released = result != null && result > 0;
        
        if (released) {
            log.info("스케줄러 락 해제: scheduler={}, instance={}", schedulerName, instanceId);
        } else {
            log.debug("스케줄러 락 해제 실패: scheduler={}, instance={} (이미 만료되었거나 다른 인스턴스)", 
                    schedulerName, instanceId);
        }
        
        return released;
    }
    
    /**
     * 락 상태 확인
     * @param lockKey 락 키
     * @return 현재 락을 보유한 값 (없으면 null)
     */
    public String getLockHolder(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 락 남은 만료 시간 조회
     * @param lockKey 락 키
     * @return 남은 시간 (초), 락이 없으면 -1
     */
    public long getLockTTL(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
    
    /**
     * 강제 락 해제 (관리자용)
     * @param lockKey 락 키
     * @return 삭제 성공 여부
     */
    public boolean forceUnlock(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        Boolean result = redisTemplate.delete(key);
        
        boolean deleted = Boolean.TRUE.equals(result);
        
        if (deleted) {
            log.warn("분산 락 강제 해제: key={}", lockKey);
        }
        
        return deleted;
    }
    
    /**
     * 모든 스케줄러 락 조회 (모니터링용)
     */
    public java.util.Map<String, Object> getAllSchedulerLocks() {
        java.util.Map<String, Object> locks = new java.util.HashMap<>();
        
        try {
            java.util.Set<String> keys = redisTemplate.keys(SCHEDULER_LOCK_PREFIX + "*");
            
            if (keys != null) {
                for (String key : keys) {
                    String schedulerName = key.substring(SCHEDULER_LOCK_PREFIX.length());
                    String holder = redisTemplate.opsForValue().get(key);
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    
                    java.util.Map<String, Object> lockInfo = new java.util.HashMap<>();
                    lockInfo.put("holder", holder);
                    lockInfo.put("ttl", ttl);
                    
                    locks.put(schedulerName, lockInfo);
                }
            }
            
        } catch (Exception e) {
            log.error("스케줄러 락 조회 중 오류: {}", e.getMessage());
        }
        
        return locks;
    }
    
    /**
     * 만료된 락 정리 (정기 작업용)
     */
    public void cleanupExpiredLocks() {
        try {
            java.util.Set<String> allLockKeys = redisTemplate.keys(LOCK_PREFIX + "*");
            int cleanedCount = 0;
            
            if (allLockKeys != null) {
                for (String key : allLockKeys) {
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttl != null && ttl <= 0) {
                        redisTemplate.delete(key);
                        cleanedCount++;
                    }
                }
            }
            
            if (cleanedCount > 0) {
                log.info("만료된 분산 락 정리 완료: {} 개", cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("분산 락 정리 중 오류: {}", e.getMessage());
        }
    }
}