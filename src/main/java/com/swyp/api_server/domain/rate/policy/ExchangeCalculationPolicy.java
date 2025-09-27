package com.swyp.api_server.domain.rate.policy;

import com.swyp.api_server.domain.rate.dto.request.ExchangeCalculationRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResultResponseDTO;
import com.swyp.api_server.entity.BankExchangeInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 환율 계산 정책 클래스
 * - 복잡한 환전 계산 로직을 명확한 비즈니스 규칙으로 분리
 * - 스프레드, 우대율, 수수료 적용 로직 캡슐화
 */
@Slf4j
@Component
public class ExchangeCalculationPolicy {
    
    /**
     * 환전 계산 결과
     */
    @Getter
    @Builder
    public static class CalculationResult {
        private final BigDecimal appliedRate;      // 최종 적용 환율
        private final BigDecimal exchangedAmount;  // 환전 금액 (수수료 제외)
        private final BigDecimal totalFee;        // 총 수수료
        private final ExchangeResultResponseDTO.FeeDetail feeDetail; // 수수료 상세
        private final BigDecimal finalAmount;     // 최종 금액 (수수료 차감 후)
        private final boolean isViable;           // 환전 가능 여부
        private final String warningMessage;     // 경고 메시지 (선택사항)
    }
    
    /**
     * 환전 계산 실행
     * @param baseRate 기준 환율
     * @param inputAmount 입력 금액
     * @param direction 환전 방향
     * @param bankInfo 은행 정보
     * @return 계산 결과
     */
    public CalculationResult calculate(BigDecimal baseRate, BigDecimal inputAmount, 
                                     ExchangeCalculationRequestDTO.ExchangeDirection direction,
                                     BankExchangeInfo bankInfo) {
        
        // 1단계: 스프레드 적용 환율 계산
        BigDecimal spreadAppliedRate = calculateSpreadAppliedRate(baseRate, direction, bankInfo);
        
        // 2단계: 우대율 적용 최종 환율 계산  
        BigDecimal finalRate = calculatePreferentialRate(spreadAppliedRate, direction, bankInfo);
        
        // 3단계: 환전 방향에 따른 수수료 및 최종 금액 계산
        return calculateByDirection(inputAmount, finalRate, direction, bankInfo);
    }
    
    /**
     * 환전 방향에 따른 수수료 및 최종 금액 계산
     */
    private CalculationResult calculateByDirection(BigDecimal inputAmount, BigDecimal finalRate,
                                                 ExchangeCalculationRequestDTO.ExchangeDirection direction,
                                                 BankExchangeInfo bankInfo) {
        
        if (direction == ExchangeCalculationRequestDTO.ExchangeDirection.KRW_TO_FOREIGN) {
            // 원화 → 외화: 입력금액(원화)에서 수수료 차감 후 환전
            return calculateKrwToForeign(inputAmount, finalRate, bankInfo);
        } else {
            // 외화 → 원화: 환전 후 수수료 차감
            return calculateForeignToKrw(inputAmount, finalRate, bankInfo);
        }
    }
    
    /**
     * 원화 → 외화 환전 계산
     */
    private CalculationResult calculateKrwToForeign(BigDecimal inputAmount, BigDecimal finalRate,
                                                  BankExchangeInfo bankInfo) {
        
        // 1단계: 수수료 계산 (원화 기준으로 대략 계산)
        BigDecimal estimatedExchangeAmount = inputAmount.divide(finalRate, 4, RoundingMode.HALF_UP);
        ExchangeResultResponseDTO.FeeDetail feeDetail = calculateFeeDetail(estimatedExchangeAmount, bankInfo);
        BigDecimal totalFee = feeDetail.getFixedFee().add(feeDetail.getRateBasedFee());
        
        // 2단계: 수수료를 차감한 후 실제 환전
        BigDecimal amountAfterFee = inputAmount.subtract(totalFee);
        
        if (amountAfterFee.compareTo(BigDecimal.ZERO) <= 0) {
            return CalculationResult.builder()
                .appliedRate(finalRate)
                .exchangedAmount(BigDecimal.ZERO)
                .totalFee(totalFee)
                .feeDetail(feeDetail)
                .finalAmount(BigDecimal.ZERO)
                .isViable(false)
                .warningMessage("수수료가 입력금액을 초과")
                .build();
        }
        
        // 3단계: 수수료 차감 후 최종 환전금액 계산
        BigDecimal finalAmount = amountAfterFee.divide(finalRate, 4, RoundingMode.HALF_UP);
        
        return CalculationResult.builder()
            .appliedRate(finalRate)
            .exchangedAmount(finalAmount)
            .totalFee(totalFee)
            .feeDetail(feeDetail)
            .finalAmount(finalAmount)
            .isViable(true)
            .warningMessage(null)
            .build();
    }
    
    /**
     * 외화 → 원화 환전 계산
     */
    private CalculationResult calculateForeignToKrw(BigDecimal inputAmount, BigDecimal finalRate,
                                                  BankExchangeInfo bankInfo) {
        
        // 1단계: 환전 (외화 → 원화)
        BigDecimal exchangedAmount = inputAmount.multiply(finalRate);
        
        // 2단계: 수수료 계산 (환전된 원화 기준)
        ExchangeResultResponseDTO.FeeDetail feeDetail = calculateFeeDetail(exchangedAmount, bankInfo);
        BigDecimal totalFee = feeDetail.getFixedFee().add(feeDetail.getRateBasedFee());
        
        // 3단계: 수수료 차감 후 최종 금액
        BigDecimal finalAmount = exchangedAmount.subtract(totalFee);
        
        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return CalculationResult.builder()
                .appliedRate(finalRate)
                .exchangedAmount(exchangedAmount)
                .totalFee(totalFee)
                .feeDetail(feeDetail)
                .finalAmount(BigDecimal.ZERO)
                .isViable(false)
                .warningMessage("수수료가 환전금액을 초과")
                .build();
        }
        
        return CalculationResult.builder()
            .appliedRate(finalRate)
            .exchangedAmount(exchangedAmount)
            .totalFee(totalFee)
            .feeDetail(feeDetail)
            .finalAmount(finalAmount)
            .isViable(true)
            .warningMessage(null)
            .build();
    }
    
    /**
     * 스프레드 적용 환율 계산
     */
    private BigDecimal calculateSpreadAppliedRate(BigDecimal baseRate, 
                                                ExchangeCalculationRequestDTO.ExchangeDirection direction, 
                                                BankExchangeInfo bankInfo) {
        BigDecimal spreadAmount = baseRate.multiply(bankInfo.getSpreadRate())
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        
        if (direction == ExchangeCalculationRequestDTO.ExchangeDirection.FOREIGN_TO_KRW) {
            // 외화 → 원화 (살 때): 기준환율 - 스프레드
            return baseRate.subtract(spreadAmount);
        } else {
            // 원화 → 외화 (팔 때): 기준환율 + 스프레드  
            return baseRate.add(spreadAmount);
        }
    }
    
    /**
     * 우대율 적용 최종 환율 계산
     */
    private BigDecimal calculatePreferentialRate(BigDecimal spreadAppliedRate,
                                               ExchangeCalculationRequestDTO.ExchangeDirection direction,
                                               BankExchangeInfo bankInfo) {
        // 기준 환율에서 스프레드를 적용한 후, 스프레드 금액의 우대율만큼 할인
        // 간단한 계산: (스프레드율 × 우대율 / 100) 만큼 추가 혜택
        BigDecimal baseRate = spreadAppliedRate; // 기준이 되는 환율
        BigDecimal spreadAmount = baseRate.multiply(bankInfo.getSpreadRate())
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
        BigDecimal preferentialDiscount = spreadAmount.multiply(bankInfo.getPreferentialRate())
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        
        if (direction == ExchangeCalculationRequestDTO.ExchangeDirection.FOREIGN_TO_KRW) {
            // 외화 → 원화: 우대율만큼 더 많이 받음
            return spreadAppliedRate.add(preferentialDiscount);
        } else {
            // 원화 → 외화: 우대율만큼 더 적게 냄
            return spreadAppliedRate.subtract(preferentialDiscount);
        }
    }
    
    /**
     * 수수료 상세 계산
     */
    private ExchangeResultResponseDTO.FeeDetail calculateFeeDetail(BigDecimal exchangedAmount, 
                                                                 BankExchangeInfo bankInfo) {
        BigDecimal fixedFee = bankInfo.getFixedFee();
        BigDecimal rateBasedFee = exchangedAmount.multiply(bankInfo.getFeeRate())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        return ExchangeResultResponseDTO.FeeDetail.builder()
            .fixedFee(fixedFee)
            .feeRate(bankInfo.getFeeRate())
            .rateBasedFee(rateBasedFee)
            .build();
    }
}