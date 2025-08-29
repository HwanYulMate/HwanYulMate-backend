package com.swyp.api_server.domain.rate.controller;

import com.swyp.api_server.domain.rate.dto.request.BankExchangeInfoRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.BankExchangeInfoResponseDTO;
import com.swyp.api_server.domain.rate.service.BankExchangeInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 은행 환전 정보 관리 컨트롤러 (관리자용)
 * - 은행별 환율 정보 CRUD 기능
 */
@Tag(name = "은행 정보 관리 API (관리자)", description = "은행별 환율 정보 관리 (관리자 전용)")
@RestController
@RequestMapping("/admin/api/bank-info")
@RequiredArgsConstructor
public class BankExchangeInfoController {
    
    private final BankExchangeInfoService bankInfoService;
    
    /**
     * 모든 은행 정보 조회 (활성화된 것만)
     */
    @Operation(summary = "활성화된 은행 정보 목록 조회", 
               description = "현재 활성화된 모든 은행의 환전 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 은행 목록을 조회함"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<List<BankExchangeInfoResponseDTO>> getAllActiveBanks() {
        List<BankExchangeInfoResponseDTO> banks = bankInfoService.getAllActiveBanks();
        return ResponseEntity.ok(banks);
    }
    
    /**
     * 특정 은행 정보 조회
     */
    @Operation(summary = "특정 은행 정보 조회", 
               description = "ID로 특정 은행의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 은행 정보를 조회함"),
        @ApiResponse(responseCode = "404", description = "해당 은행 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BankExchangeInfoResponseDTO> getBankInfo(
            @Parameter(description = "은행 정보 ID", required = true, example = "1")
            @PathVariable Long id) {
        
        BankExchangeInfoResponseDTO bankInfo = bankInfoService.getBankInfo(id);
        return ResponseEntity.ok(bankInfo);
    }
    
    /**
     * 은행명으로 조회
     */
    @Operation(summary = "은행명으로 조회", 
               description = "은행명으로 해당 은행의 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 은행 정보를 조회함"),
        @ApiResponse(responseCode = "404", description = "해당 은행 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<BankExchangeInfoResponseDTO> getBankInfoByName(
            @Parameter(description = "은행명", required = true, example = "KB국민은행")
            @RequestParam String bankName) {
        
        BankExchangeInfoResponseDTO bankInfo = bankInfoService.getBankInfoByName(bankName);
        return ResponseEntity.ok(bankInfo);
    }
    
    /**
     * 새 은행 정보 등록
     */
    @Operation(summary = "새 은행 정보 등록", 
               description = "새로운 은행의 환전 정보를 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "성공적으로 은행 정보를 등록함"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 중복된 은행 정보"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public ResponseEntity<BankExchangeInfoResponseDTO> createBankInfo(
            @Valid @RequestBody BankExchangeInfoRequestDTO requestDTO) {
        
        BankExchangeInfoResponseDTO createdBank = bankInfoService.createBankInfo(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBank);
    }
    
    /**
     * 은행 정보 수정
     */
    @Operation(summary = "은행 정보 수정", 
               description = "기존 은행의 환전 정보를 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 은행 정보를 수정함"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "해당 은행 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BankExchangeInfoResponseDTO> updateBankInfo(
            @Parameter(description = "은행 정보 ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody BankExchangeInfoRequestDTO requestDTO) {
        
        BankExchangeInfoResponseDTO updatedBank = bankInfoService.updateBankInfo(id, requestDTO);
        return ResponseEntity.ok(updatedBank);
    }
    
    /**
     * 은행 활성화/비활성화 토글
     */
    @Operation(summary = "은행 활성화/비활성화", 
               description = "은행의 활성화 상태를 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 상태를 변경함"),
        @ApiResponse(responseCode = "404", description = "해당 은행 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<BankExchangeInfoResponseDTO> toggleBankStatus(
            @Parameter(description = "은행 정보 ID", required = true, example = "1")
            @PathVariable Long id) {
        
        BankExchangeInfoResponseDTO updatedBank = bankInfoService.toggleBankStatus(id);
        return ResponseEntity.ok(updatedBank);
    }
    
    /**
     * 은행 정보 삭제 (비활성화)
     */
    @Operation(summary = "은행 정보 삭제", 
               description = "은행 정보를 삭제합니다. (실제로는 비활성화 처리)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "성공적으로 삭제함"),
        @ApiResponse(responseCode = "404", description = "해당 은행 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBankInfo(
            @Parameter(description = "은행 정보 ID", required = true, example = "1")
            @PathVariable Long id) {
        
        bankInfoService.deleteBankInfo(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 온라인 환전 가능한 은행 목록
     */
    @Operation(summary = "온라인 환전 가능 은행 목록", 
               description = "온라인에서 환전이 가능한 은행들의 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 온라인 가능 은행 목록을 조회함"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/online-available")
    public ResponseEntity<List<BankExchangeInfoResponseDTO>> getOnlineAvailableBanks() {
        List<BankExchangeInfoResponseDTO> banks = bankInfoService.getOnlineAvailableBanks();
        return ResponseEntity.ok(banks);
    }
}