package com.swyp.api_server.domain.rate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 은행 환전 정보 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BankExchangeInfoResponse", description = "은행 환전 정보 응답")
public class BankExchangeInfoResponseDTO {
    
    @Schema(description = "은행 정보 ID", example = "1")
    private Long id;
    
    @Schema(description = "은행명", example = "KB국민은행")
    private String bankName;
    
    @Schema(description = "은행 코드", example = "004")
    private String bankCode;
    
    @Schema(description = "스프레드율 (%)", example = "1.5")
    private BigDecimal spreadRate;
    
    @Schema(description = "우대율 (%)", example = "50.0")
    private BigDecimal preferentialRate;
    
    @Schema(description = "고정 수수료 (원)", example = "1000.0")
    private BigDecimal fixedFee;
    
    @Schema(description = "수수료율 (%)", example = "0.1")
    private BigDecimal feeRate;
    
    @Schema(description = "최소 환전 금액", example = "100.0")
    private BigDecimal minAmount;
    
    @Schema(description = "최대 환전 금액", example = "10000.0")
    private BigDecimal maxAmount;
    
    @Schema(description = "온라인 환전 가능 여부", example = "true")
    private Boolean isOnlineAvailable;
    
    @Schema(description = "서비스 활성화 여부", example = "true")
    private Boolean isActive;
    
    @Schema(description = "부가 설명", example = "인터넷뱅킹 50% 우대율 적용")
    private String description;
    
    @Schema(description = "화면 표시 순서", example = "1")
    private Integer displayOrder;
    
    @Schema(description = "생성일시")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;
}