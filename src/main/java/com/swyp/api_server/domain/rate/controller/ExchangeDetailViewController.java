package com.swyp.api_server.domain.rate.controller;

import com.swyp.api_server.domain.rate.dto.request.ExchangeRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/api")
@Tag(name = "Exchange Rate Detail API")
public class ExchangeDetailViewController {

    // 실시간 환율 + 등락율
    @GetMapping("/exchange/realtime")
    @Operation(summary = "실시간 환율 및 등락률 조회",
               description = "선택한 통화의 현재 환율과 전 거래일 대비 등락률을 조회합니다.")
    public ResponseEntity<ExchangeRealtimeResponseDTO> exchangeDetail(ExchangeRequestDTO exchangeRequestDTO) {
        ExchangeRealtimeResponseDTO exchangeResponseDTO = new ExchangeRealtimeResponseDTO();
        return ResponseEntity.ok(exchangeResponseDTO);
    }

    // 차트 데이터
    @GetMapping("/exchange/chart")
    @Operation(summary = "환율 차트 데이터 조회",
               description = "차트 시각화를 위한 환율 시계열 데이터를 제공합니다. 당일 및 과거 데이터 조회를 지원합니다.")
    public ResponseEntity<List<ExchangeChartResponseDTO>> exchangeChartDetail(ExchangeRequestDTO exchangeRequestDTO) {
        List<ExchangeChartResponseDTO> exchangeChartResponseDTO = new ArrayList<>();
        return ResponseEntity.ok(exchangeChartResponseDTO);
    }

    // 최근 1주일 환율 변동
    @GetMapping("/exchange/weekly")
    @Operation(summary = "최근 1주일 환율 변동 조회",
               description = "최근 7일간의 환율 변동 정보를 제공합니다. 일별 최고가, 최저가, 종가 정보를 포함합니다.")
    public ResponseEntity<ExchangeWeeklyResponseDTO> exchangeWeeklyDetail(ExchangeRequestDTO exchangeRequestDTO) {
        ExchangeWeeklyResponseDTO exchangeWeeklyResponseDTO = new ExchangeWeeklyResponseDTO();
        return ResponseEntity.ok(exchangeWeeklyResponseDTO);
    }

    // 최근 1개월 환율 변동
    @GetMapping("exchange/monthly")
    @Operation(summary = "최근 1개월 환율 변동 조회",
               description = "최근 30일간의 환율 변동 정보를 제공합니다. 일별 최고가, 최저가, 종가 정보를 포함합니다.")
    public ResponseEntity<ExchangeMonthlyResponseDTO> exchangeMonthlyDetail(ExchangeRequestDTO exchangeRequestDTO) {
        ExchangeMonthlyResponseDTO exchangeMonthlyResponseDTO = new ExchangeMonthlyResponseDTO();
        return ResponseEntity.ok(exchangeMonthlyResponseDTO);
    }

    // 뉴스
    @GetMapping("/exchange/news")
    @Operation(summary = "환율 관련 최신 뉴스 조회",
               description = "검증된 금융 뉴스 제공처에서 수집한 환율 관련 최신 뉴스 기사를 제공합니다.")
    public ResponseEntity<List<ExchangeNewsListResponseDTO>>  exchangeNewsList() {
        List<ExchangeNewsListResponseDTO> exchangeNewsListResponseDTO = new ArrayList<>();
        return ResponseEntity.ok(exchangeNewsListResponseDTO);
    }

    // 통화별 뉴스
    @GetMapping("/exchange/news/detail")
    @Operation(summary = "통화별 환율 관련 최신 뉴스 조회",
            description = "통화별 검증된 금융 뉴스 제공처에서 수집한 환율 관련 최신 뉴스 기사를 제공합니다.")
    public ResponseEntity<List<ExchangeNewsListResponseDTO>>  exchangeNewsListDetail(ExchangeRequestDTO exchangeRequestDTO  ) {
        List<ExchangeNewsListResponseDTO> exchangeNewsListResponseDTO = new ArrayList<>();
        return ResponseEntity.ok(exchangeNewsListResponseDTO);
    }


}
