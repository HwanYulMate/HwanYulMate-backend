package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.service.BankExchangeInfoService;
import com.swyp.api_server.entity.BankExchangeInfo;
import com.swyp.api_server.domain.rate.dto.request.ExchangeCalculationRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResultResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 환전 계산 서비스 구현체
 * - 실시간 환율 정보를 기반으로 은행별 환전 예상 금액 계산
 * - 우대율 및 수수료 적용하여 실제 환전 결과 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeCalculationServiceImpl implements ExchangeCalculationService {
    
    private final ExchangeRateService exchangeRateService;
    private final BankExchangeInfoService bankInfoService;
    
    @Override
    @Cacheable(value = "exchangeCalculation", key = "#request.currencyCode + '_' + #request.amount + '_' + #request.direction")
    public List<ExchangeResultResponseDTO> calculateExchangeRates(ExchangeCalculationRequestDTO request) {
        return calculateExchangeRatesWithoutCache(request);
    }
    
    public List<ExchangeResultResponseDTO> calculateExchangeRatesWithoutCache(ExchangeCalculationRequestDTO request) {
        try {
            // 통화 유효성 검증
            validateRequest(request);
            
            // 실시간 환율 및 기준 날짜 조회
            ExchangeRateInfo rateInfo = getCurrentExchangeRateWithDate(request.getCurrencyCode());
            
            // DB에서 은행별 환율 정보 조회
            List<BankExchangeInfo> bankRates = bankInfoService.getAllActiveBankEntities();
            
            // 특정 은행 필터링
            if (request.getSpecificBank() != null && !request.getSpecificBank().trim().isEmpty()) {
                bankRates = bankRates.stream()
                    .filter(bank -> bank.getBankName().equals(request.getSpecificBank()))
                    .collect(Collectors.toList());
                    
                if (bankRates.isEmpty()) {
                    throw new CustomException(ErrorCode.UNSUPPORTED_BANK, 
                        "지원하지 않는 은행입니다: " + request.getSpecificBank());
                }
            }
            
            // 은행별 환전 결과 계산
            List<ExchangeResultResponseDTO> results = bankRates.stream()
                .map(bankInfo -> calculateSingleExchange(request, bankInfo, rateInfo))
                .sorted(Comparator.comparing(ExchangeResultResponseDTO::getFinalAmount).reversed())
                .collect(Collectors.toList());
            
            log.info("환전 계산 완료: {} {}, 결과 {}개", request.getCurrencyCode(), 
                request.getAmount(), results.size());
            
            return results;
            
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("환전 계산 중 오류 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "환전 계산 실패", e);
        }
    }
    
    @Override
    public ExchangeResultResponseDTO calculateExchangeRate(ExchangeCalculationRequestDTO request, String bankName) {
        request.setSpecificBank(bankName);
        List<ExchangeResultResponseDTO> results = calculateExchangeRates(request);
        
        if (results.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                "해당 은행의 환전 정보를 찾을 수 없습니다: " + bankName);
        }
        
        return results.get(0);
    }
    
    /**
     * 환율 및 기준 날짜 정보를 담는 내부 클래스
     */
    private static class ExchangeRateInfo {
        private final BigDecimal rate;
        private final String baseDate;
        
        public ExchangeRateInfo(BigDecimal rate, String baseDate) {
            this.rate = rate;
            this.baseDate = baseDate;
        }
        
        public BigDecimal getRate() {
            return rate;
        }
        
        public String getBaseDate() {
            return baseDate;
        }
    }
    
    /**
     * 단일 은행의 환전 결과 계산
     */
    private ExchangeResultResponseDTO calculateSingleExchange(
            ExchangeCalculationRequestDTO request, BankExchangeInfo bankInfo, ExchangeRateInfo rateInfo) {
        
        // 기준 환율에 스프레드 적용
        BigDecimal marketRate = rateInfo.getRate();  // 실시간 환율 사용
        BigDecimal spreadAmount = marketRate.multiply(bankInfo.getSpreadRate())
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        
        BigDecimal spreadAppliedRate;
        if (request.getDirection() == ExchangeCalculationRequestDTO.ExchangeDirection.FOREIGN_TO_KRW) {
            // 외화 → 원화 (살 때): 기준환율 - 스프레드
            spreadAppliedRate = marketRate.subtract(spreadAmount);
        } else {
            // 원화 → 외화 (팔 때): 기준환율 + 스프레드  
            spreadAppliedRate = marketRate.add(spreadAmount);
        }
        
        // 우대율 적용
        BigDecimal preferentialDiscount = spreadAmount.multiply(bankInfo.getPreferentialRate())
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
        BigDecimal finalRate;
        if (request.getDirection() == ExchangeCalculationRequestDTO.ExchangeDirection.FOREIGN_TO_KRW) {
            // 외화 → 원화: 우대율만큼 더 많이 받음
            finalRate = spreadAppliedRate.add(preferentialDiscount);
        } else {
            // 원화 → 외화: 우대율만큼 더 적게 냄
            finalRate = spreadAppliedRate.subtract(preferentialDiscount);
        }
        
        // 환전 금액 계산 (방향별 계산 로직)
        BigDecimal exchangedAmount;
        
        if (request.getDirection() == ExchangeCalculationRequestDTO.ExchangeDirection.FOREIGN_TO_KRW) {
            // 외화 → 원화: 외화 × 환율 = 원화
            if (isHundredUnitCurrency(request.getCurrencyCode())) {
                // JPY, IDR은 100단위로 제공되므로 실제 계산 시 단위 조정
                // 예: 10,000엔 환전 = (10,000 ÷ 100) × (100엔당 환율)
                BigDecimal adjustedAmount = request.getAmount().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                exchangedAmount = adjustedAmount.multiply(finalRate);
            } else {
                exchangedAmount = request.getAmount().multiply(finalRate);
            }
        } else {
            // 원화 → 외화: 원화 ÷ 환율 = 외화
            if (isHundredUnitCurrency(request.getCurrencyCode())) {
                // JPY, IDR은 100단위이므로 결과에 100을 곱해줌
                exchangedAmount = request.getAmount().divide(finalRate, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            } else {
                exchangedAmount = request.getAmount().divide(finalRate, 4, RoundingMode.HALF_UP);
            }
        }
        
        // 수수료 계산
        ExchangeResultResponseDTO.FeeDetail feeDetail = calculateFee(exchangedAmount, bankInfo);
        BigDecimal totalFee = feeDetail.getFixedFee().add(feeDetail.getRateBasedFee());
        
        // 최종 금액 (수수료 제외)
        BigDecimal finalAmount = exchangedAmount.subtract(totalFee);
        
        // 최소/최대 금액 검증
        validateExchangeAmount(request.getAmount(), bankInfo);
        
        return ExchangeResultResponseDTO.builder()
            .bankName(bankInfo.getBankName())
            .bankCode(bankInfo.getBankCode())
            .baseRate(marketRate)
            .appliedRate(finalRate)
            .preferentialRate(bankInfo.getPreferentialRate())
            .spreadRate(bankInfo.getSpreadRate())
            .totalFee(totalFee)
            .feeDetail(feeDetail)
            .finalAmount(finalAmount)
            .inputAmount(request.getAmount())
            .currencyCode(request.getCurrencyCode())
            .flagImageUrl(getFlagImageUrl(request.getCurrencyCode()))
            .isOnlineAvailable(bankInfo.getIsOnlineAvailable())
            .description(bankInfo.getDescription())
            .baseDate(rateInfo.getBaseDate())
            .build();
    }
    
    /**
     * 수수료 계산
     */
    private ExchangeResultResponseDTO.FeeDetail calculateFee(BigDecimal amount, BankExchangeInfo bankInfo) {
        BigDecimal fixedFee = bankInfo.getFixedFee();
        BigDecimal rateBasedFee = amount.multiply(bankInfo.getFeeRate())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        return ExchangeResultResponseDTO.FeeDetail.builder()
            .fixedFee(fixedFee)
            .feeRate(bankInfo.getFeeRate())
            .rateBasedFee(rateBasedFee)
            .build();
    }
    
    /**
     * 현재 환율 및 기준 날짜 조회 - DB 전용 (API 호출 없음)
     * 스케줄러가 수집한 캐시된 데이터만 사용
     */
    private ExchangeRateInfo getCurrentExchangeRateWithDate(String currencyCode) {
        try {
            log.info("계산기용 환율 조회 (캐시/DB): {}", currencyCode);
            
            // 캐시된 환율 데이터 사용 (API 호출 없음)
            List<ExchangeResponseDTO> rates = exchangeRateService.getAllExchangeRates();
            
            ExchangeResponseDTO rateData = rates.stream()
                .filter(rate -> rate.getCurrencyCode().equals(currencyCode))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_RATE_NOT_FOUND, 
                    "환율 정보를 찾을 수 없습니다. 스케줄러 동작을 확인해주세요: " + currencyCode));
            
            return new ExchangeRateInfo(rateData.getExchangeRate(), rateData.getBaseDate());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("환율 조회 실패: {}", currencyCode, e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_API_ERROR, 
                "환율 조회 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 요청 유효성 검증
     */
    private void validateRequest(ExchangeCalculationRequestDTO request) {
        if (request.getCurrencyCode() == null || request.getCurrencyCode().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "통화 코드는 필수입니다.");
        }
        
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "환전 금액은 0보다 커야 합니다.");
        }
    }
    
    /**
     * 환전 금액 범위 검증
     */
    private void validateExchangeAmount(BigDecimal amount, BankExchangeInfo bankInfo) {
        if (amount.compareTo(bankInfo.getMinAmount()) < 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                String.format("%s 최소 환전 금액은 %s %s입니다.", 
                    bankInfo.getBankName(), bankInfo.getMinAmount(), "USD"));
        }
        
        if (amount.compareTo(bankInfo.getMaxAmount()) > 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST,
                String.format("%s 최대 환전 금액은 %s %s입니다.", 
                    bankInfo.getBankName(), bankInfo.getMaxAmount(), "USD"));
        }
    }
    
    /**
     * 100단위로 제공되는 통화인지 확인
     */
    private boolean isHundredUnitCurrency(String currencyCode) {
        return "JPY".equals(currencyCode) || "IDR".equals(currencyCode);
    }
    
    /**
     * 통화 코드에 해당하는 국기 이미지 URL 조회
     */
    private String getFlagImageUrl(String currencyCode) {
        try {
            return com.swyp.api_server.domain.rate.ExchangeList.ExchangeType
                .valueOf(currencyCode.toUpperCase()).getFlagImageUrl();
        } catch (IllegalArgumentException e) {
            return "/images/flags/default.png"; // 기본 이미지 반환
        }
    }
}