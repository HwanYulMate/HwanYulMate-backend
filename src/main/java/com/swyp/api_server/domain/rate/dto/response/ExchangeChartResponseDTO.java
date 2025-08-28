package com.swyp.api_server.domain.rate.dto.response;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(name = "ExchangeChartResponse", description = "환율 차트 데이터 응답")
@Getter
@Setter
public class ExchangeChartResponseDTO {
    @Schema(description = "통화 이름 명", example = "USD")
    private String exchangeName;

    @Schema(description = "기준 일시(로컬 시간)", example = "2025-08-12T09:00:00")
    private LocalDateTime exchangeDate;

    @Schema(description = "해당 시점 환율", example = "1385.23")
    private double exchangeRate;
}
