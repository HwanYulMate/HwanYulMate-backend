package com.swyp.api_server.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Cloud Messaging (FCM) 설정
 * - iOS 푸시 알림 발송을 위한 Firebase 초기화
 */
@Configuration
@Log4j2
public class FCMConfig {

    @Value("${fcm.service-account-file:hwanyulmate-firebase-service-account.json}")
    private String serviceAccountFile;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource(serviceAccountFile);
                
                if (!resource.exists()) {
                    log.warn("FCM 서비스 계정 파일을 찾을 수 없습니다: {}. 푸시 알림 기능이 비활성화됩니다.", serviceAccountFile);
                    return;
                }
                
                InputStream serviceAccount = resource.getInputStream();
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase가 성공적으로 초기화되었습니다.");
                
            }
        } catch (IOException e) {
            log.error("Firebase 초기화 중 오류 발생", e);
        }
    }
}