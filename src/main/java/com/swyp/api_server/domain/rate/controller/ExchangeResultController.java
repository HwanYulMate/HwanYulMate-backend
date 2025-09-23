package com.swyp.api_server.domain.rate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.swyp.api_server.common.dto.ErrorResponse;

import com.swyp.api_server.domain.rate.dto.request.ExchangeCalculationRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResultResponseDTO;
import com.swyp.api_server.domain.rate.service.ExchangeCalculationService;
import com.swyp.api_server.domain.rate.service.ExchangeCalculationServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ExchangeResultController {
    
    private final ExchangeCalculationService exchangeCalculationService;

    /**
     * 은행별 환전 예상 금액 비교
     */
    @Operation(
        summary = "은행별 환전 예상 금액 비교 (DB 기반)",
        description = "DB에 저장된 최신 환율 정보를 기반으로 여러 은행의 환전 예상 금액을 비교합니다. " +
                      "환율 데이터는 스케줄러에 의해 수집되고 개별 통화 캐시를 통해 빠른 응답속도를 제공하며, 우대율과 수수료가 모두 반영됩니다. " +
                      "Policy 패턴을 적용하여 복잡한 계산 로직을 명확히 분리했으며, " +
                      "**중요**: 수수료가 환전금액을 초과하는 경우 최종 금액은 0원으로 표시되며, 음수 결과는 발생하지 않습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 환전 결과를 반환함",
            content = @Content(
                schema = @Schema(implementation = ExchangeResultResponseDTO.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "환전 계산 결과 (업데이트된 실제 우대율)",
                    value = "[\n" +
                            "  {\n" +
                            "    \"bankName\": \"우리은행\",\n" +
                            "    \"bankCode\": \"020\",\n" +
                            "    \"baseRate\": 1385.20,\n" +
                            "    \"appliedRate\": 1396.50,\n" +
                            "    \"preferentialRate\": 90.0,\n" +
                            "    \"spreadRate\": 1.0,\n" +
                            "    \"totalFee\": 0.0,\n" +
                            "    \"finalAmount\": 139650.0,\n" +
                            "    \"inputAmount\": 100.0,\n" +
                            "    \"currencyCode\": \"USD\",\n" +
                            "    \"flagImageUrl\": \"/images/flags/us.png\",\n" +
                            "    \"isOnlineAvailable\": true,\n" +
                            "    \"description\": \"WiBee뱅킹 최대 90% 우대율, 수수료 무료\",\n" +
                            "    \"baseDate\": \"20250920\"\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"bankName\": \"SC제일은행\",\n" +
                            "    \"bankCode\": \"023\",\n" +
                            "    \"baseRate\": 1385.20,\n" +
                            "    \"appliedRate\": 1365.80,\n" +
                            "    \"preferentialRate\": 40.0,\n" +
                            "    \"spreadRate\": 1.4,\n" +
                            "    \"totalFee\": 2500.0,\n" +
                            "    \"finalAmount\": 0.0,\n" +
                            "    \"inputAmount\": 1.0,\n" +
                            "    \"currencyCode\": \"USD\",\n" +
                            "    \"flagImageUrl\": \"/images/flags/us.png\",\n" +
                            "    \"isOnlineAvailable\": null,\n" +
                            "    \"description\": \"SC인터넷뱅킹 최대 40% 우대율 (수수료가 환전금액을 초과)\",\n" +
                            "    \"baseDate\": \"20250920\"\n" +
                            "  }\n" +
                            "]"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 (유효하지 않은 통화 코드, 금액, 또는 수수료가 환전금액 초과)",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "수수료 초과 오류",
                    value = "{\n" +
                            "  \"success\": false,\n" +
                            "  \"code\": \"RATE_009\",\n" +
                            "  \"message\": \"수수료가 환전 금액을 초과합니다. 더 큰 금액으로 환전을 시도해주세요.\",\n" +
                            "  \"detail\": \"1달러 환전시 수수료 2,500원 발생\",\n" +
                            "  \"timestamp\": \"2025-09-20T10:30:00\",\n" +
                            "  \"path\": \"/api/exchange/calculate\"\n" +
                            "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "환율 API 서비스 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/exchange/calculate")
    public ResponseEntity<List<ExchangeResultResponseDTO>> calculateExchangeRates(
            @Valid @RequestBody ExchangeCalculationRequestDTO request) {
        
        log.info("환전 계산 요청: currencyCode={}, amount={}, direction={}", 
                request.getCurrencyCode(), request.getAmount(), request.getDirection());
        
        List<ExchangeResultResponseDTO> results = exchangeCalculationService.calculateExchangeRates(request);
        return ResponseEntity.ok(results);
    }

    /**
     * 간편 환전 계산 (GET 방식)
     */
    @Operation(
        summary = "간편 환전 계산 (DB 기반)",
        description = "GET 방식으로 간단하게 환전 예상 금액을 DB 기반으로 계산합니다. " +
                      "스케줄러가 수집한 최신 환율 정보와 개별 통화 캐시를 사용하여 빠른 응답을 제공합니다. 기본값: 외화→원화 방향. " +
                      "**주의**: 소액 환전시 수수료가 환전금액을 초과할 수 있으며, 이 경우 최종 금액은 0원으로 표시됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 환전 결과를 반환함",
            content = @Content(schema = @Schema(implementation = ExchangeResultResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 (유효하지 않은 통화 코드 또는 금액)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "환율 API 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/exchange/calculate")
    public ResponseEntity<List<ExchangeResultResponseDTO>> calculateExchangeRatesSimple(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @RequestParam String currencyCode,
            
            @Parameter(description = "환전할 금액", example = "1000", required = true)
            @RequestParam BigDecimal amount,
            
            @Parameter(description = "환전 방향", example = "KRW_TO_FOREIGN")
            @RequestParam(defaultValue = "KRW_TO_FOREIGN") ExchangeCalculationRequestDTO.ExchangeDirection direction,
            
            @Parameter(description = "특정 은행 지정 (선택사항)", example = "KB국민은행")
            @RequestParam(required = false) String bank) {
        
        log.info("GET 환전 계산 요청: currencyCode={}, amount={}, direction={}", 
                currencyCode, amount, direction);
        
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
        summary = "특정 은행 환전 계산 (DB 기반)",
        description = "지정한 은행의 환전 예상 금액을 DB 기반으로 계산합니다. " +
                      "스케줄러가 수집한 최신 환율 정보를 사용하여 빠른 계산 결과를 제공합니다. " +
                      "실제 우대율 반영: 우리은행(90%), IM뱅크(80%), KB국민은행(75%), 하나은행(70%), 신한은행(60%), 씨티은행(50%), SC제일은행(40%)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 환전 결과를 반환함",
            content = @Content(schema = @Schema(implementation = ExchangeResultResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 또는 지원하지 않는 은행",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "해당 은행의 환율 정보를 찾을 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "환율 API 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/exchange/calculate/{bankName}")
    public ResponseEntity<ExchangeResultResponseDTO> calculateSpecificBankRate(
            @Parameter(description = "은행명", example = "KB국민은행", required = true)
            @PathVariable String bankName,
            
            @Valid @RequestBody ExchangeCalculationRequestDTO request) {
        
        ExchangeResultResponseDTO result = exchangeCalculationService.calculateExchangeRate(request, bankName);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 캐시 우회 환전 계산 (디버깅용)
     */
    @Operation(
        summary = "캐시 우회 환전 계산 (DB 기반)",
        description = "디버깅용 API로 캐시를 우회하여 DB에서 직접 환율을 조회해 환전 예상 금액을 계산합니다. " +
                      "외부 API 호출 없이 DB 데이터만 사용하여 계산합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 환전 결과를 반환함",
            content = @Content(schema = @Schema(implementation = ExchangeResultResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 (유효하지 않은 통화 코드 또는 금액)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "환율 API 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/exchange/calculate-nocache")
    public ResponseEntity<List<ExchangeResultResponseDTO>> calculateExchangeRatesNoCache(
            @RequestParam String currencyCode,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "FOREIGN_TO_KRW") ExchangeCalculationRequestDTO.ExchangeDirection direction,
            @RequestParam(required = false) String bank) {
        
        ExchangeCalculationRequestDTO request = ExchangeCalculationRequestDTO.builder()
                .currencyCode(currencyCode.toUpperCase())
                .amount(amount)
                .direction(direction)
                .specificBank(bank)
                .build();
        
        ExchangeCalculationServiceImpl serviceImpl = (ExchangeCalculationServiceImpl) exchangeCalculationService;
        List<ExchangeResultResponseDTO> results = serviceImpl.calculateExchangeRatesWithoutCache(request);
        return ResponseEntity.ok(results);
    }
}
