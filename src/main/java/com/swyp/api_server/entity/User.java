package com.swyp.api_server.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String provider; // google, apple, local
    private String providerId;
    private String role; // ROLE_USER, ROLE_ADMIN
    private String userName;

    @CreatedDate
    private LocalDateTime createdAt;
    
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;        // 탈퇴 여부
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;          // 탈퇴 요청 시간
    
    @Column(name = "final_deletion_date")
    private LocalDateTime finalDeletionDate;  // 실제 삭제 예정 날짜 (탈퇴 + 30일)
    
    @Column(name = "withdrawal_reason", length = 500)
    private String withdrawalReason;          // 탈퇴 이유 (선택사항)
    
    @Column(name = "fcm_token")
    private String fcmToken;  // FCM 푸시 알림용 디바이스 토큰
    
    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
    
    /**
     * 회원 탈퇴 처리 (즉시 삭제 아닌 30일 보관)
     */
    public void withdraw(String reason) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.finalDeletionDate = LocalDateTime.now().plusDays(30);
        this.withdrawalReason = reason;
    }
    
    /**
     * 탈퇴 취소 (30일 내 복구)
     */
    public void cancelWithdrawal() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.finalDeletionDate = null;
    }


}
