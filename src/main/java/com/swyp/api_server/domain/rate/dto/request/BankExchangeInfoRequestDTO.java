package com.swyp.api_server.domain.rate.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 은행 환전 정보 요청 DTO (관리자용)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BankExchangeInfoRequest", description = "은행 환전 정보 등록/수정 요청")
public class BankExchangeInfoRequestDTO {
    
    @NotBlank(message = "은행명은 필수입니다.")
    @Size(max = 50, message = "은행명은 50자 이내여야 합니다.")
    @Schema(description = "은행명", example = "우리은행", required = true)
    private String bankName;
    
    @NotBlank(message = "은행 코드는 필수입니다.")
    @Size(max = 10, message = "은행 코드는 10자 이내여야 합니다.")
    @Schema(description = "은행 코드", example = "020", required = true)
    private String bankCode;
    
    @NotNull(message = "스프레드율은 필수입니다.")
    @DecimalMin(value = "0.0", message = "스프레드율은 0 이상이어야 합니다.")
    @DecimalMax(value = "10.0", message = "스프레드율은 10% 이하여야 합니다.")
    @Schema(description = "스프레드율 (%)", example = "1.0", required = true)
    private BigDecimal spreadRate;
    
    @NotNull(message = "우대율은 필수입니다.")
    @DecimalMin(value = "0.0", message = "우대율은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.0", message = "우대율은 100% 이하여야 합니다.")
    @Schema(description = "우대율 (%) - 실제 은행 우대율 반영", example = "90.0", required = true)
    private BigDecimal preferentialRate;
    
    @NotNull(message = "고정 수수료는 필수입니다.")
    @DecimalMin(value = "0.0", message = "고정 수수료는 0 이상이어야 합니다.")
    @Schema(description = "고정 수수료 (원)", example = "0.0", required = true)
    private BigDecimal fixedFee;
    
    @NotNull(message = "수수료율은 필수입니다.")
    @DecimalMin(value = "0.0", message = "수수료율은 0 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "수수료율은 5% 이하여야 합니다.")
    @Schema(description = "수수료율 (%)", example = "0.1", required = true)
    private BigDecimal feeRate;
    
    @NotNull(message = "최소 환전 금액은 필수입니다.")
    @DecimalMin(value = "1.0", message = "최소 환전 금액은 1 이상이어야 합니다.")
    @Schema(description = "최소 환전 금액", example = "100.0", required = true)
    private BigDecimal minAmount;
    
    @NotNull(message = "최대 환전 금액은 필수입니다.")
    @DecimalMin(value = "1.0", message = "최대 환전 금액은 1 이상이어야 합니다.")
    @Schema(description = "최대 환전 금액", example = "10000.0", required = true)
    private BigDecimal maxAmount;
    
    @NotNull(message = "온라인 환전 가능 여부는 필수입니다.")
    @Schema(description = "온라인 환전 가능 여부", example = "true", required = true)
    private Boolean isOnlineAvailable;
    
    @Size(max = 200, message = "설명은 200자 이내여야 합니다.")
    @Schema(description = "부가 설명", example = "WiBee뱅킹 최대 90% 우대율, 수수료 무료")
    private String description;
    
    @Min(value = 0, message = "표시 순서는 0 이상이어야 합니다.")
    @Schema(description = "화면 표시 순서", example = "1")
    @Builder.Default
    private Integer displayOrder = 0;
    
    /**
     * 유효성 검증 - 최소/최대 금액 관계 확인
     */
    @AssertTrue(message = "최대 환전 금액은 최소 환전 금액보다 커야 합니다.")
    public boolean isValidAmountRange() {
        if (minAmount == null || maxAmount == null) {
            return true; // null 체크는 @NotNull이 처리
        }
        return maxAmount.compareTo(minAmount) > 0;
    }
}