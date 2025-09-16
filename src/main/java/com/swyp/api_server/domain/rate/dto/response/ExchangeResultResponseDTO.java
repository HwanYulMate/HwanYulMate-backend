package com.swyp.api_server.domain.rate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(name = "ExchangeResultResponse", description = "환전 결과 계산 응답")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeResultResponseDTO implements Serializable {

    @Schema(description = "은행명", example = "KB국민은행")
    private String bankName;
    
    @Schema(description = "은행 코드", example = "004")
    private String bankCode;

    @Schema(description = "기준 환율", example = "1385.20")
    private BigDecimal baseRate;
    
    @Schema(description = "실제 적용 환율 (우대율 반영)", example = "1380.50")
    private BigDecimal appliedRate;

    @Schema(description = "우대율 (%)", example = "50.0")
    private BigDecimal preferentialRate;
    
    @Schema(description = "스프레드율", example = "1.5")
    private BigDecimal spreadRate;

    @Schema(description = "수수료 금액", example = "1000.0")
    private BigDecimal totalFee;
    
    @Schema(description = "수수료 상세")
    private FeeDetail feeDetail;

    @Schema(description = "최종 환전 결과 금액 (원화)", example = "1379500.0")
    private BigDecimal finalAmount;
    
    @Schema(description = "입력 금액 (외화)", example = "1000.0")
    private BigDecimal inputAmount;
    
    @Schema(description = "통화 코드", example = "USD")
    private String currencyCode;
    
    @Schema(description = "국기 이미지 URL", example = "/images/flags/us.png")
    private String flagImageUrl;
    
    @Schema(description = "온라인 환전 가능 여부", example = "true")
    private boolean isOnlineAvailable;
    
    @Schema(description = "추가 설명", example = "인터넷뱅킹 50% 우대율 적용")
    private String description;
    
    @Schema(description = "환율 기준 날짜", example = "20250916")
    private String baseDate;
    
    /**
     * 수수료 상세 정보
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "수수료 상세 정보")
    public static class FeeDetail implements Serializable {
        
        @Schema(description = "고정 수수료", example = "1000.0")
        private BigDecimal fixedFee;
        
        @Schema(description = "수수료율 (%)", example = "0.1")
        private BigDecimal feeRate;
        
        @Schema(description = "수수료율 적용 금액", example = "138.0")
        private BigDecimal rateBasedFee;
    }
}
