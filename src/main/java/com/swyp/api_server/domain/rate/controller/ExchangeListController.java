package com.swyp.api_server.domain.rate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import com.swyp.api_server.domain.rate.service.ExchangeRateService;
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

    /**
     * 14개국 통화의 실시간 환율 목록 조회
     * @return 14개국 환율 정보 리스트
     */
    @GetMapping("/exchangeList")
    @Operation(summary = "실시간 환율 목록 조회", 
               description = "14개국 통화의 실시간 환율 정보를 목록 형태로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 환율 목록을 조회함"),
        @ApiResponse(responseCode = "503", description = "환율 API 서비스 오류"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<ExchangeResponseDTO>> getExchangeList() {
        // ExchangeRateService를 통해 실제 환율 데이터 조회
        List<ExchangeResponseDTO> exchangeRates = exchangeRateService.getAllExchangeRates();
        return ResponseEntity.ok(exchangeRates);
    }
}
