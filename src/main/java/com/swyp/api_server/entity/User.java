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


}
