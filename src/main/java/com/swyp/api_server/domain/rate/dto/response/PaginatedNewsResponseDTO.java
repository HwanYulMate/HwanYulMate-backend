package com.swyp.api_server.domain.rate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Schema(name = "PaginatedNews", description = "페이징된 뉴스 목록 응답")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedNewsResponseDTO {

    @Schema(description = "뉴스 목록")
    private List<ExchangeNewsListResponseDTO> newsList;

    @Schema(description = "현재 페이지", example = "1")
    private int currentPage;

    @Schema(description = "페이지 크기", example = "10")
    private int pageSize;

    @Schema(description = "총 아이템 수", example = "50")
    private int totalCount;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;
}