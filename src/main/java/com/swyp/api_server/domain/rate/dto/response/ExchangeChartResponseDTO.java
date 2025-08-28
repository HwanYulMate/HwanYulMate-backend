package com.swyp.api_server.domain.rate.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;

@Schema(name = "ExchangeChartResponse", description = "환율 차트 데이터 응답")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeChartResponseDTO {
    
    @Schema(description = "날짜 (YYYY-MM-DD)", example = "2024-01-15")
    private String date;

    @Schema(description = "해당 날짜 환율", example = "1385.23")
    private BigDecimal rate;
    
    @Schema(description = "타임스탬프")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}