package com.swyp.api_server.domain.rate.controller;

/**
 * 은행 정보 관리 API (관리자)
 * - 실시간 은행 우대율/수수료 수정 기능
 * - 캐시 무효화 자동 처리
 */
import com.swyp.api_server.domain.rate.dto.request.BankExchangeInfoRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.BankExchangeInfoResponseDTO;
import com.swyp.api_server.domain.rate.service.BankExchangeInfoService;
import com.swyp.api_server.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "은행 정보 관리 API (관리자)", description = "은행별 환율 정보 관리 (관리자 전용) - 실시간 수정 가능")
@RestController
@RequestMapping("/admin/api/bank-info")
@RequiredArgsConstructor
public class BankExchangeInfoController {
    
    private final BankExchangeInfoService bankInfoService;
    
    /**
     * 모든 은행 정보 조회
     */
    @Operation(
        summary = "모든 은행 정보 조회",
        description = "활성화된 모든 은행의 우대율, 수수료 정보를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<BankExchangeInfoResponseDTO>> getAllBanks() {
        List<BankExchangeInfoResponseDTO> banks = bankInfoService.getAllActiveBanks();
        return ResponseEntity.ok(banks);
    }
    
    /**
     * 특정 은행 정보 수정 (우대율, 수수료 등)
     */
    @Operation(
        summary = "은행 정보 수정 (실시간 반영)",
        description = "은행의 우대율, 수수료, 스프레드율 등을 실시간으로 수정합니다. 캐시는 자동으로 무효화됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 은행 정보가 수정됨",
            content = @Content(schema = @Schema(implementation = BankExchangeInfoResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "해당 은행을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<BankExchangeInfoResponseDTO> updateBankInfo(
            @Parameter(description = "은행 ID", example = "1", required = true)
            @PathVariable Long id,
            
            @Valid @RequestBody BankExchangeInfoRequestDTO request) {
        
        BankExchangeInfoResponseDTO updatedBank = bankInfoService.updateBankInfo(id, request);
        return ResponseEntity.ok(updatedBank);
    }
    
    /**
     * 은행명으로 정보 수정
     */
    @Operation(
        summary = "은행명으로 정보 수정",
        description = "은행명을 이용해 해당 은행의 정보를 수정합니다. ID를 모를 때 사용하세요."
    )
    @PutMapping("/name/{bankName}")
    public ResponseEntity<BankExchangeInfoResponseDTO> updateBankInfoByName(
            @Parameter(description = "은행명", example = "우리은행", required = true)
            @PathVariable String bankName,
            
            @Valid @RequestBody BankExchangeInfoRequestDTO request) {
        
        BankExchangeInfoResponseDTO updatedBank = bankInfoService.updateBankInfoByName(bankName, request);
        return ResponseEntity.ok(updatedBank);
    }
    
    /**
     * 캐시 수동 무효화
     */
    @Operation(
        summary = "캐시 무효화",
        description = "은행 정보 캐시를 수동으로 무효화합니다. DB 직접 수정 후 사용하세요."
    )
    @PostMapping("/cache/evict")
    public ResponseEntity<String> evictCache() {
        bankInfoService.evictAllBankCache();
        return ResponseEntity.ok("은행 정보 캐시가 무효화되었습니다.");
    }
}