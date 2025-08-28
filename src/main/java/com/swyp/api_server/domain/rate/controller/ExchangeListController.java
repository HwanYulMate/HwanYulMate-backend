package com.swyp.api_server.domain.rate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
@RestController
@RequestMapping("/v1/api")
@Tag(name = "환율 조회 API", description = "환율 목록을 조회하는 API")
public class ExchangeListController {

    @GetMapping("/exchangeList")
    @Operation(summary = "환율 목록 조회", description = "모든 환율 정보를 목록 형태로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 환율 목록을 조회함"),
        @ApiResponse(responseCode = "500", description = "서버 에러 발생")
    })
    public ResponseEntity<List<ExchangeResponseDTO>> getExchangeList(){
        List<ExchangeResponseDTO> exchangeResponseDTOList = new ArrayList<>();

        return ResponseEntity.ok(exchangeResponseDTOList);
    }
}
