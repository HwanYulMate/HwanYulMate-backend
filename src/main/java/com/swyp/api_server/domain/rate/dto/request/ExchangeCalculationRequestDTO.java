package com.swyp.api_server.domain.rate.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 환전 계산 요청 DTO
 * - 환전할 통화와 금액 정보
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExchangeCalculationRequest", description = "환전 계산 요청")
public class ExchangeCalculationRequestDTO {
    
    @NotBlank(message = "통화 코드는 필수입니다.")
    @Schema(description = "환전할 통화 코드", example = "USD", required = true)
    private String currencyCode;
    
    @NotNull(message = "환전 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "환전 금액은 0.01 이상이어야 합니다.")
    @Schema(description = "환전할 금액 (외화 기준)", example = "1000.00", required = true)
    private BigDecimal amount;
    
    @Schema(description = "환전 방향 (KRW_TO_FOREIGN: 원화→외화, FOREIGN_TO_KRW: 외화→원화)", 
            example = "FOREIGN_TO_KRW", defaultValue = "FOREIGN_TO_KRW")
    @Builder.Default
    private ExchangeDirection direction = ExchangeDirection.FOREIGN_TO_KRW;
    
    @Schema(description = "특정 은행만 조회 (선택사항)", example = "KB국민은행")
    private String specificBank;
    
    /**
     * 환전 방향 enum
     */
    public enum ExchangeDirection {
        @Schema(description = "외화 → 원화")
        FOREIGN_TO_KRW,
        
        @Schema(description = "원화 → 외화") 
        KRW_TO_FOREIGN
    }
}