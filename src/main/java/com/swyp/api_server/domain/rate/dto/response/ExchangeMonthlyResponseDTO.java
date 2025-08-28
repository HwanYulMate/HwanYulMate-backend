package com.swyp.api_server.domain.rate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(name = "ExchangeMonthlyResponse", description = "최근 30일 최고/최저 환율 데이터 응답")
@Getter
@Setter
public class ExchangeMonthlyResponseDTO {
    @Schema(description = "통화 이름 명", example = "USD")
    private String exchangeName;

    @Schema(description = "최근 30일 최저 환율", example = "1285.23")
    private double exchangeLowRate;

    @Schema(description = "최근 30일 최고 환율", example = "1385.23")
    private double exchangeHighRate;
}
