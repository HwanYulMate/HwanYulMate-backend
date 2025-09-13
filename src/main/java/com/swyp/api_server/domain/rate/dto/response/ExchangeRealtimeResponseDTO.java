package com.swyp.api_server.domain.rate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Schema(name = "ExchangeRealtimeResponse", description = "실시간 환율 정보 응답")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRealtimeResponseDTO {

    @Schema(description = "통화 코드", example = "USD")
    private String currencyCode;
    
    @Schema(description = "통화 이름", example = "미국 달러")
    private String currencyName;

    @Schema(description = "국기 이미지 URL", example = "/images/flags/us.png")
    private String flagImageUrl;

    @Schema(description = "현재 환율", example = "1385.20")
    private BigDecimal currentRate;
    
    @Schema(description = "전일 환율", example = "1387.35")
    private BigDecimal previousRate;

    @Schema(description = "등락폭 (절대값)", example = "-2.15")
    private BigDecimal changeAmount;

    @Schema(description = "등락률 (%)", example = "-0.15")
    private BigDecimal changeRate;
    
    @Schema(description = "업데이트 시각")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}