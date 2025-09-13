package com.swyp.api_server.domain.rate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.swyp.api_server.common.dto.ErrorResponse;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import com.swyp.api_server.domain.rate.dto.ExchangeRateWithChangeDto;
import com.swyp.api_server.domain.rate.service.ExchangeRateService;
import com.swyp.api_server.domain.rate.service.ExchangeRateHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * 환율 목록 조회 컨트롤러
 * - 14개국 통화의 실시간 환율 정보 제공
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "환율 조회 API", description = "환율 목록을 조회하는 API")
public class ExchangeListController {
    
    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateHistoryService historyService;

    /**
     * 14개국 통화의 실시간 환율 목록 조회
     * @return 14개국 환율 정보 리스트
     */
    @GetMapping("/exchangeList")
    @Operation(summary = "실시간 환율 목록 조회 (변동률 포함)", 
               description = "14개국 통화의 실시간 환율 정보와 전일 대비 변동률을 목록 형태로 조회합니다. 변동 금액, 변동 퍼센트, 변동 방향 정보가 포함됩니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 환율 목록을 조회함",
            content = @Content(schema = @Schema(implementation = ExchangeRateWithChangeDto.class))
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
    public ResponseEntity<List<ExchangeRateWithChangeDto>> getExchangeList() {
        // ExchangeRateHistoryService를 통해 변동률이 포함된 환율 데이터 조회
        List<ExchangeRateWithChangeDto> exchangeRates = historyService.getRatesWithChange();
        return ResponseEntity.ok(exchangeRates);
    }
}
