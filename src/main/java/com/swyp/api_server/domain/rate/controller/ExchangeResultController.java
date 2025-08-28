package com.swyp.api_server.domain.rate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.swyp.api_server.domain.rate.dto.response.ExchangeResultResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "환율 결과 API")
@RestController
@RequestMapping("/v1/api")
public class ExchangeResultController {

    // 환율 결과
    @Operation(
        summary = "환율 결과 조회",
        description = "현재 기준의 환율 결과 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 환율 결과를 반환함"),
        @ApiResponse(responseCode = "500", description = "서버 에러 발생")
    })
    @GetMapping("/exchange/result")
    public ResponseEntity<List<ExchangeResultResponseDTO>> getExchangeList(){
        List<ExchangeResultResponseDTO> exchangeResultResponseDTOList = new ArrayList<>();
        return ResponseEntity.ok(exchangeResultResponseDTOList);
    }
}
