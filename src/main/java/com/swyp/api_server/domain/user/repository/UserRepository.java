package com.swyp.api_server.domain.user.repository;

import com.swyp.api_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // OAuth 소셜 로그인 관련 쿼리
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    List<User> findAllByProviderAndProviderId(String provider, String providerId);
    
    // FCM 토큰 관련 쿼리
    List<User> findByFcmTokenIsNotNull();
    List<User> findByFcmToken(String fcmToken);
    long countByFcmTokenIsNotNull();
    
    // 중복된 FCM 토큰 조회
    @Query("SELECT u.fcmToken FROM User u WHERE u.fcmToken IS NOT NULL GROUP BY u.fcmToken HAVING COUNT(u.fcmToken) > 1")
    List<String> findDuplicateFcmTokens();
}
