package com.swyp.api_server.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 설정 클래스
 * - JWT 기반 인증 체계 구성
 * - 세션 비활성화 (Stateless)
 * - 인증 불필요 경로 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;  // JWT 인증 필터
    private final CorsConfigurationSource corsConfigurationSource;  // CORS 설정

    /**
     * 비밀번호 암호화를 위한 BCryptPasswordEncoder 빈 등록
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 설정
     * @param http HttpSecurity 객체
     * @return 설정된 SecurityFilterChain
     * @throws Exception 설정 오류 시 발생
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용하므로 REST API에서는 불필요)
                .csrf(csrf -> csrf.disable())
                
                // CORS 설정 적용 (Swagger UI 및 프론트엔드 개발용)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // 세션 사용하지 않음 (JWT 기반 Stateless 인증)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL별 인증/인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인, 토큰갱신은 누구나 접근 가능
                        .requestMatchers("/api/signup", "/api/login").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        // OAuth 로그인 관련 엔드포인트는 인증 비필요
                        .requestMatchers("/api/oauth/**").permitAll()
                        // 환율 관련 조회 API는 공개 (인증 불필요)
                        .requestMatchers("/api/exchangeList").permitAll()
                        .requestMatchers("/api/exchange/realtime").permitAll()
                        .requestMatchers("/api/exchange/chart").permitAll()
                        .requestMatchers("/api/exchange/weekly").permitAll()
                        .requestMatchers("/api/exchange/monthly").permitAll()
                        .requestMatchers("/api/exchange/3months").permitAll()
                        .requestMatchers("/api/exchange/6months").permitAll()
                        .requestMatchers("/api/exchange/yearly").permitAll()
                        .requestMatchers("/api/exchange/calculate/**").permitAll()
                        .requestMatchers("/api/exchange/news/**").permitAll()
                        // 피드백 유형 조회는 공개
                        .requestMatchers("/api/feedback/types").permitAll()
                        // 정적 리소스 (이미지 등) 접근 허용
                        .requestMatchers("/images/**").permitAll()
                        // Swagger UI 문서화 접근 허용
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        // 개인 설정 API는 인증 필요 (/api/auth/**, /api/alert/**)
                        // 관리자 API는 인증 필요 (/admin/api/**)
                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 전에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
