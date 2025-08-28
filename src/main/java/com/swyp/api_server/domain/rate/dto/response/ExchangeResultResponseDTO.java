package com.swyp.api_server.domain.rate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;


@Schema(name = "ExchangeResultResponse", description = "환전 결과 계산 응답")
@Getter
@Setter
public class ExchangeResultResponseDTO {

    @Schema(description = "은행/제공처 명", example = "KB Bank")
    private String bankName;

    @Schema(description = "적용 환율(Exchange Rate)", example = "1385.20")
    private double exchangePrice;

    @Schema(description = "수수료 금액(Fee)", example = "3000.0")
    private double fee;

    @Schema(description = "우대율(스프레드 할인, %)", example = "80.0")
    private double spreadDiscount;

    @Schema(description = "최종 환전 결과 금액", example = "1385200.0")
    private double result;
}
