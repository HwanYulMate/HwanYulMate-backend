package com.swyp.api_server.domain.rate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(name = "ExchangeRealtimeResponse", description = "실시간 환율 정보 응답")
@Getter
@Setter
public class ExchangeRealtimeResponseDTO {

    @Schema(description = "환율 제공처(거래소/은행) 명", example = "KB Bank")
    private String exchangeName;

    @Schema(description = "현재 환율(가격)", example = "1385.20")
    private double exchangePrice;

    @Schema(description = "전일 대비 절대 등락값", example = "-2.15")
    private double exchangeChangePrice;

    @Schema(description = "전일 대비 등락률(%)", example = "-0.15")
    private double exchangeChangePercent;


}
