package com.swyp.api_server.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // 인증/인가 규칙
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/signup").permitAll() // 회원가입은 인증 없이 허용
                        .anyRequest().authenticated() // 그 외는 인증 필요
                )

                // 기본 폼 로그인은 사용 안함 (REST API니까)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
