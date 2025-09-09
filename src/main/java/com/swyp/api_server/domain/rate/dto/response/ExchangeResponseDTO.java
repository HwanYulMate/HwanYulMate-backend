package com.swyp.api_server.domain.rate.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExchangeResponse", description = "단일 통화 환율 응답")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeResponseDTO implements Serializable {

    @Schema(description = "통화 코드", example = "USD")
    private String currencyCode;

    @Schema(description = "통화 이름", example = "미국 달러")
    private String currencyName;

    @Schema(description = "국기 이미지 URL", example = "/images/flags/us.svg")
    private String flagImageUrl;

    @Schema(description = "환율 값 (1 외화 = ? 원)", example = "1380.25")
    private BigDecimal exchangeRate;

    @Schema(description = "기준 날짜", example = "2025-08-11")
    private String baseDate;
}
