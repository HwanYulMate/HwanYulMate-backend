package com.swyp.api_server.domain.rate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(name = "ExchangeNews", description = "환율 관련 뉴스 항목 응답")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeNewsListResponseDTO {

    @Schema(description = "뉴스 제목", example = "원/달러 환율 장중 1,390원 돌파")
    private String title;

    @Schema(description = "뉴스 요약/설명", example = "미국 CPI 발표를 앞두고 환율 변동성 확대")
    private String description;

    @Schema(description = "뉴스 링크 URL", example = "https://n.news.naver.com/article/12345")
    private String link;
    
    @Schema(description = "원문 기사 URL", example = "https://news.example.com/article/12345")
    private String originalLink;

    @Schema(description = "발행 일시", example = "Mon, 15 Jan 2024 10:30:00 +0900")
    private String pubDate;
}