package com.swyp.api_server.domain.rate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.swyp.api_server.domain.rate.dto.request.ExchangeCalculationRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResultResponseDTO;
import com.swyp.api_server.domain.rate.service.ExchangeCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 환전 예상 금액 비교 컨트롤러
 * - 은행별 환전 우대율 및 수수료 비교
 * - 실시간 환율 기반 환전 예상 금액 계산
 */
@Tag(name = "환전 금액 비교 API", description = "은행별 환전 예상 금액 비교 및 계산")
@RestController
@RequestMapping("/v1/api")
@RequiredArgsConstructor
public class ExchangeResultController {
    
    private final ExchangeCalculationService exchangeCalculationService;

    /**
     * 은행별 환전 예상 금액 비교
     */
    @Operation(
        summary = "은행별 환전 예상 금액 비교",
        description = "실시간 환율 정보를 기반으로 여러 은행의 환전 예상 금액을 비교합니다. 우대율과 수수료가 모두 반영됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 환전 결과를 반환함"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 통화 코드 또는 금액)"),
        @ApiResponse(responseCode = "503", description = "환율 API 서비스 오류"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/exchange/calculate")
    public ResponseEntity<List<ExchangeResultResponseDTO>> calculateExchangeRates(
            @Valid @RequestBody ExchangeCalculationRequestDTO request) {
        
        List<ExchangeResultResponseDTO> results = exchangeCalculationService.calculateExchangeRates(request);
        return ResponseEntity.ok(results);
    }

    /**
     * 간편 환전 계산 (GET 방식)
     */
    @Operation(
        summary = "간편 환전 계산",
        description = "GET 방식으로 간단하게 환전 예상 금액을 계산합니다. 기본값: 외화→원화 방향"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 환전 결과를 반환함"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 에러 발생")
    })
    @GetMapping("/exchange/calculate")
    public ResponseEntity<List<ExchangeResultResponseDTO>> calculateExchangeRatesSimple(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @RequestParam String currencyCode,
            
            @Parameter(description = "환전할 금액", example = "1000", required = true)
            @RequestParam BigDecimal amount,
            
            @Parameter(description = "환전 방향", example = "FOREIGN_TO_KRW")
            @RequestParam(defaultValue = "FOREIGN_TO_KRW") ExchangeCalculationRequestDTO.ExchangeDirection direction,
            
            @Parameter(description = "특정 은행 지정 (선택사항)", example = "KB국민은행")
            @RequestParam(required = false) String bank) {
        
        ExchangeCalculationRequestDTO request = ExchangeCalculationRequestDTO.builder()
                .currencyCode(currencyCode.toUpperCase())
                .amount(amount)
                .direction(direction)
                .specificBank(bank)
                .build();
        
        List<ExchangeResultResponseDTO> results = exchangeCalculationService.calculateExchangeRates(request);
        return ResponseEntity.ok(results);
    }
    
    /**
     * 특정 은행의 환전 계산
     */
    @Operation(
        summary = "특정 은행 환전 계산",
        description = "지정한 은행의 환전 예상 금액을 계산합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 환전 결과를 반환함"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 지원하지 않는 은행"),
        @ApiResponse(responseCode = "500", description = "서버 에러 발생")
    })
    @PostMapping("/exchange/calculate/{bankName}")
    public ResponseEntity<ExchangeResultResponseDTO> calculateSpecificBankRate(
            @Parameter(description = "은행명", example = "KB국민은행", required = true)
            @PathVariable String bankName,
            
            @Valid @RequestBody ExchangeCalculationRequestDTO request) {
        
        ExchangeResultResponseDTO result = exchangeCalculationService.calculateExchangeRate(request, bankName);
        return ResponseEntity.ok(result);
    }
}
