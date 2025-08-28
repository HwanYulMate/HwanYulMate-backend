
package com.swyp.api_server.domain.rate.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ExchangeRequest", description = "환율 요청 DTO")
public class ExchangeRequestDTO {
    @Schema(description = "통화 이름", example = "USD")
    private String exchangeName;
}
