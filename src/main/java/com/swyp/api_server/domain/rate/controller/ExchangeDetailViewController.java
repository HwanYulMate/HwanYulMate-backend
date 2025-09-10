package com.swyp.api_server.domain.rate.controller;

import com.swyp.api_server.common.dto.ErrorResponse;
import com.swyp.api_server.domain.rate.dto.request.ExchangeRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.*;
import com.swyp.api_server.domain.rate.service.ExchangeRateService;
import com.swyp.api_server.domain.rate.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 환율 상세 정보 컨트롤러
 * - 특정 통화의 상세 환율 정보 제공
 * - 실시간 환율, 차트 데이터, 과거 환율, 관련 뉴스 제공
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Exchange Rate Detail API", description = "환율 상세 정보 API")
public class ExchangeDetailViewController {
    
    private final ExchangeRateService exchangeRateService;
    private final NewsService newsService;

    /**
     * 특정 통화의 실시간 환율 및 등락률 조회
     * @param currencyCode 통화 코드 (USD, JPY, EUR 등)
     * @return 실시간 환율과 등락률 정보
     */
    @GetMapping("/exchange/realtime")
    @Operation(summary = "실시간 환율 및 등락률 조회",
               description = "선택한 통화의 현재 환율과 전일 대비 등락률을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 실시간 환율을 조회함",
            content = @Content(schema = @Schema(implementation = ExchangeRealtimeResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "지원하지 않는 통화 코드입니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "해당 통화의 환율 정보를 찾을 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "환율 정보 조회 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ExchangeRealtimeResponseDTO> getRealtimeExchange(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @RequestParam String currencyCode) {
        
        ExchangeRealtimeResponseDTO realtimeData = exchangeRateService.getRealtimeExchangeRate(currencyCode);
        return ResponseEntity.ok(realtimeData);
    }

    /**
     * 특정 통화의 차트 데이터 조회 (최근 30일)
     * @param currencyCode 통화 코드
     * @return 차트용 시계열 데이터
     */
    @GetMapping("/exchange/chart")
    @Operation(summary = "환율 차트 데이터 조회",
               description = "차트 시각화를 위한 환율 시계열 데이터를 제공합니다. 최근 30일간의 데이터를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 차트 데이터를 조회함",
            content = @Content(schema = @Schema(implementation = ExchangeChartResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "지원하지 않는 통화 코드입니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "해당 통화의 환율 정보를 찾을 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "환율 정보 조회 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<List<ExchangeChartResponseDTO>> getExchangeChart(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @RequestParam String currencyCode) {
        
        List<ExchangeChartResponseDTO> chartData = exchangeRateService.getExchangeChartData(currencyCode);
        return ResponseEntity.ok(chartData);
    }

    /**
     * 특정 통화의 최근 1주일 환율 변동 조회
     * @param currencyCode 통화 코드
     * @return 최근 7일간의 환율 데이터
     */
    @GetMapping("/exchange/weekly")
    @Operation(summary = "최근 1주일 환율 변동 조회",
               description = "최근 7일간의 환율 변동 정보를 제공합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 주간 환율 데이터를 조회함",
            content = @Content(schema = @Schema(implementation = ExchangeChartResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "지원하지 않는 통화 코드입니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "해당 통화의 환율 정보를 찾을 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "환율 정보 조회 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<List<ExchangeChartResponseDTO>> getWeeklyExchange(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @RequestParam String currencyCode) {
        
        List<ExchangeChartResponseDTO> weeklyData = exchangeRateService.getHistoricalExchangeRate(currencyCode, 7);
        return ResponseEntity.ok(weeklyData);
    }

    /**
     * 특정 통화의 최근 1개월 환율 변동 조회
     * @param currencyCode 통화 코드
     * @return 최근 30일간의 환율 데이터
     */
    @GetMapping("/exchange/monthly")
    @Operation(summary = "최근 1개월 환율 변동 조회",
               description = "최근 30일간의 환율 변동 정보를 제공합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 월간 환율 데이터를 조회함",
            content = @Content(schema = @Schema(implementation = ExchangeChartResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "지원하지 않는 통화 코드입니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "해당 통화의 환율 정보를 찾을 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "환율 정보 조회 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<List<ExchangeChartResponseDTO>> getMonthlyExchange(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @RequestParam String currencyCode) {
        
        List<ExchangeChartResponseDTO> monthlyData = exchangeRateService.getHistoricalExchangeRate(currencyCode, 30);
        return ResponseEntity.ok(monthlyData);
    }

    // /**
    //  * 환율 관련 최신 뉴스 조회
    //  * @return 환율 관련 뉴스 리스트
    //  */
    // @GetMapping("/exchange/news")
    // @Operation(summary = "환율 관련 최신 뉴스 조회",
    //            description = "검증된 금융 뉴스 제공처에서 수집한 환율 관련 최신 뉴스 기사를 제공합니다.")
    // public ResponseEntity<List<ExchangeNewsListResponseDTO>> getExchangeNewsList() {
    //     // 전체 환율 관련 뉴스 조회
    //     List<ExchangeNewsListResponseDTO> newsList = newsService.getExchangeNews();
    //     return ResponseEntity.ok(newsList);
    // }

    // /**
    //  * 특정 통화 관련 뉴스 조회
    //  * @param currencyCode 통화 코드
    //  * @return 통화별 뉴스 리스트
    //  */
    // @GetMapping("/exchange/news/detail")
    // @Operation(summary = "통화별 환율 관련 최신 뉴스 조회",
    //            description = "특정 통화와 관련된 환율 뉴스 기사를 제공합니다.")
    // public ResponseEntity<List<ExchangeNewsListResponseDTO>> getCurrencyNewsList(
    //         @Parameter(description = "통화 코드", example = "USD", required = true)
    //         @RequestParam String currencyCode) {
    //     
    //     // 특정 통화 관련 뉴스 조회
    //     List<ExchangeNewsListResponseDTO> newsList = newsService.getCurrencyNews(currencyCode);
    //     return ResponseEntity.ok(newsList);
    // }

    /**
     * 환율 관련 뉴스 페이징 조회 (무한스크롤용)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 환율 뉴스
     */
    @GetMapping("/exchange/news/paginated")
    @Operation(summary = "환율 관련 뉴스 페이징 조회",
               description = "무한스크롤을 위한 페이징된 환율 관련 뉴스를 제공합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 페이징된 뉴스를 조회함",
            content = @Content(schema = @Schema(implementation = PaginatedNewsResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 페이지 번호 또는 크기입니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "뉴스 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<PaginatedNewsResponseDTO> getExchangeNewsPaginated(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") 
            @RequestParam(defaultValue = "10") int size) {
        
        PaginatedNewsResponseDTO paginatedNews = newsService.getExchangeNewsPaginated(page, size);
        return ResponseEntity.ok(paginatedNews);
    }

    /**
     * 특정 통화 관련 뉴스 페이징 조회 (무한스크롤용)
     * @param currencyCode 통화 코드
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 통화별 뉴스
     */
    @GetMapping("/exchange/news/detail/paginated")
    @Operation(summary = "통화별 뉴스 페이징 조회",
               description = "무한스크롤을 위한 특정 통화 관련 페이징된 뉴스를 제공합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 통화별 페이징된 뉴스를 조회함",
            content = @Content(schema = @Schema(implementation = PaginatedNewsResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "지원하지 않는 통화 코드이거나 잘못된 페이지 정보입니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "해당 통화의 뉴스 정보를 찾을 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "뉴스 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<PaginatedNewsResponseDTO> getCurrencyNewsPaginated(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @RequestParam String currencyCode,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        PaginatedNewsResponseDTO paginatedNews = newsService.getCurrencyNewsPaginated(currencyCode, page, size);
        return ResponseEntity.ok(paginatedNews);
    }

    /**
     * 환율 뉴스 검색 (무한스크롤용)
     * @param searchKeyword 검색 키워드
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 검색된 환율 뉴스
     */
    @GetMapping("/exchange/news/search")
    @Operation(summary = "환율 뉴스 검색",
               description = "사용자 입력 키워드로 환율 관련 뉴스를 검색합니다. 키워드에 '환율'이 자동으로 추가됩니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 뉴스 검색 결과를 조회함",
            content = @Content(schema = @Schema(implementation = PaginatedNewsResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "검색 키워드가 비어있거나 잘못된 페이지 정보입니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "뉴스 검색 서비스가 일시적으로 이용할 수 없습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버에서 예상치 못한 오류가 발생했습니다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<PaginatedNewsResponseDTO> searchExchangeNews(
            @Parameter(description = "검색 키워드", example = "달러", required = true)
            @RequestParam String searchKeyword,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        PaginatedNewsResponseDTO searchResults = newsService.searchExchangeNews(searchKeyword, page, size);
        return ResponseEntity.ok(searchResults);
    }

}
