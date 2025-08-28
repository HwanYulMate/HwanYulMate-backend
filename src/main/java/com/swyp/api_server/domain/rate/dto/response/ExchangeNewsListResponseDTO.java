package com.swyp.api_server.domain.rate.dto.response;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

//환율 뉴스 리스트
@Schema(name = "ExchangeNews", description = "환율 관련 뉴스 항목 응답")
@Getter
@Setter
public class ExchangeNewsListResponseDTO {

    @Schema(description = "뉴스 제목", example = "원/달러 환율 장중 1,390원 돌파")
    private String newsTitle;

    @Schema(description = "뉴스 요약/설명", example = "미국 CPI 발표를 앞두고 환율 변동성 확대")
    private String newsDescription;

    @Schema(description = "원문 기사 URL", example = "https://news.example.com/article/12345")
    private String newsUrl;

    @Schema(description = "기사 기준 일시(로컬 시간)", example = "2025-08-12T09:00:00")
    private LocalDateTime newsDate;
}
