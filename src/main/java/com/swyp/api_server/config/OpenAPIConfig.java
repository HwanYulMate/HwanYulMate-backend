package com.swyp.api_server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 (Swagger) 설정
 * - API 문서화 및 JWT 인증 스키마 설정
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🏦 HwanYulMate API")
                        .description("# 💰 실시간 환율 조회 및 환전 예상 금액 비교 서비스 API\n\n" +
                                "**SWYP 2기 HwanYulMate 팀**의 iOS 환율 앱 백엔드 서비스입니다.\n\n" +
                                "## 🚀 주요 기능\n" +
                                "- 💱 **실시간 환율 조회**: 12개국 주요 통화 환율 정보 (한국수출입은행 공식 데이터)\n" +
                                "- 📊 **환율 차트 분석**: 평일 기준 7일~1년 기간별 환율 변동 차트 (자동 히스토리 수집)\n" +
                                "- 🏛️ **환전 예상 금액 비교**: 7개 은행별 환전 우대율 및 수수료 비교\n" +
                                "- 🔔 **환율 알림 설정**: 목표 환율 달성 및 일일 환율 알림 (스케줄러 기반)\n" +
                                "- 📰 **환율 뉴스**: 실시간 환율 관련 뉴스 검색 및 조회 (무한스크롤)\n" +
                                "- 🔐 **JWT 인증**: Access/Refresh Token 기반 보안 인증 (24h/30일)\n" +
                                "- 🍎 **Apple 로그인**: Apple OAuth 인증 및 완전한 로그아웃/탈퇴 지원\n\n" +
                                "## 📊 데이터 소스 및 수집 방식\n" +
                                "- **환율 정보**: 한국수출입은행 공식 API (스케줄러 기반 자동 수집)\n" +
                                "- **히스토리 데이터**: 단계별 자동 확장 (30일→90일→180일→365일)\n" +
                                "- **수집 스케줄**: 평일 오전 9:30, 오후 3:00 (API 호출량 최적화)\n" +
                                "- **뉴스 정보**: 네이버 뉴스 검색 API (일일 25,000회 한도)\n" +
                                "- **캐싱**: Redis 기반 성능 최적화 (5분 TTL)\n\n" +
                                "## 🛠️ 기술 스택\n" +
                                "- **Backend**: Spring Boot 3.x + Java 17\n" +
                                "- **Database**: MySQL 8.0 + JPA/Hibernate\n" +
                                "- **Cache**: Redis 7.0\n" +
                                "- **Container**: Docker + Docker Compose\n" +
                                "- **Security**: Spring Security + JWT\n\n" +
                                "## 👨‍💻 개발팀\n" +
                                "- **Backend 개발**: 전우선 (단독 개발)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("전우선 (Backend Developer)")
                                .email("jeonwooseon@naver.com")
                                .url("https://github.com/wooxexn"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("로컬 개발 서버")
                ))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Access Token을 Authorization 헤더에 'Bearer {token}' 형식으로 전송")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}