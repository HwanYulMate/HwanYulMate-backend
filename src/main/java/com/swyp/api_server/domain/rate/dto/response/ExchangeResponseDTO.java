package com.swyp.api_server.domain.rate.dto.response;

import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "ExchangeResponse", description = "단일 통화 환율 응답")
public class ExchangeResponseDTO {

    @Schema(description = "통화 코드", example = "USD")
    private String currencyCode;

    @Schema(description = "통화 이름", example = "미국 달러")
    private String currencyName;

    @Schema(description = "환율 값", example = "1380.25")
    private double rate;

    @Schema(description = "기준 날짜", example = "2025-08-11")
    private String baseDate;
}
