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

    @Schema(description = "은행명", example = "우리은행")
    private String bankName;
    
    @Schema(description = "은행 코드", example = "020")
    private String bankCode;

    @Schema(description = "기준 환율", example = "1385.20")
    @Builder.Default
    private BigDecimal baseRate = BigDecimal.ZERO;
    
    @Schema(description = "실제 적용 환율 (우대율 반영)", example = "1396.50")
    @Builder.Default
    private BigDecimal appliedRate = BigDecimal.ZERO;

    @Schema(description = "우대율 (%) - 실제 은행 우대율 적용", example = "90.0")
    @Builder.Default
    private BigDecimal preferentialRate = BigDecimal.ZERO;
    
    @Schema(description = "스프레드율", example = "1.0")
    @Builder.Default
    private BigDecimal spreadRate = BigDecimal.ZERO;

    @Schema(description = "수수료 금액", example = "0.0")
    @Builder.Default
    private BigDecimal totalFee = BigDecimal.ZERO;
    
    @Schema(description = "수수료 상세")
    private FeeDetail feeDetail;

    @Schema(description = "최종 환전 결과 금액 (수수료 제외 후) - 음수인 경우 0.0으로 표시", example = "139650.0")
    @Builder.Default
    private BigDecimal finalAmount = BigDecimal.ZERO;
    
    @Schema(description = "입력 금액 (외화)", example = "1000.0")
    @Builder.Default
    private BigDecimal inputAmount = BigDecimal.ZERO;
    
    @Schema(description = "통화 코드", example = "USD")
    private String currencyCode;
    
    @Schema(description = "국기 이미지 URL", example = "/images/flags/us.png")
    private String flagImageUrl;
    
    @Schema(description = "온라인 환전 가능 여부", example = "true", nullable = true)
    private Boolean isOnlineAvailable;
    
    @Schema(description = "추가 설명", example = "WiBee뱅킹 최대 90% 우대율, 수수료 무료")
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
        
        @Schema(description = "고정 수수료", example = "0.0", defaultValue = "0.0")
        @Builder.Default
        private BigDecimal fixedFee = BigDecimal.ZERO;
        
        @Schema(description = "수수료율 (%)", example = "0.0", defaultValue = "0.0")
        @Builder.Default
        private BigDecimal feeRate = BigDecimal.ZERO;
        
        @Schema(description = "수수료율 적용 금액", example = "0.0", defaultValue = "0.0")
        @Builder.Default
        private BigDecimal rateBasedFee = BigDecimal.ZERO;
    }
}
