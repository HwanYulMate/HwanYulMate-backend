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
        description = "활성화된 모든 은행의 우대율, 수수료 정보를 조회합니다. 관리자가 현재 설정된 은행 정보를 확인할 때 사용합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 은행 정보 목록을 조회함",
            content = @Content(
                schema = @Schema(implementation = BankExchangeInfoResponseDTO.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "은행 정보 목록",
                    value = "[\n" +
                            "  {\n" +
                            "    \"id\": 1,\n" +
                            "    \"bankName\": \"우리은행\",\n" +
                            "    \"bankCode\": \"020\",\n" +
                            "    \"spreadRate\": 1.0,\n" +
                            "    \"preferentialRate\": 90.0,\n" +
                            "    \"fixedFee\": 0.0,\n" +
                            "    \"feeRate\": 0.0,\n" +
                            "    \"minAmount\": 100.0,\n" +
                            "    \"maxAmount\": 10000.0,\n" +
                            "    \"isOnlineAvailable\": true,\n" +
                            "    \"isActive\": true,\n" +
                            "    \"description\": \"WiBee뱅킹 최대 90% 우대율, 수수료 무료\",\n" +
                            "    \"displayOrder\": 1\n" +
                            "  }\n" +
                            "]"
                )
            )
        )
    })
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
        description = "은행의 우대율, 수수료, 스프레드율 등을 실시간으로 수정합니다. " +
                      "수정 후 즉시 환율 계산에 반영되며, 캐시는 자동으로 무효화됩니다. " +
                      "**주의**: 수정된 정보는 즉시 모든 환율 계산 API에 영향을 줍니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 은행 정보가 수정됨",
            content = @Content(
                schema = @Schema(implementation = BankExchangeInfoResponseDTO.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "수정된 은행 정보",
                    value = "{\n" +
                            "  \"id\": 1,\n" +
                            "  \"bankName\": \"우리은행\",\n" +
                            "  \"bankCode\": \"020\",\n" +
                            "  \"spreadRate\": 1.0,\n" +
                            "  \"preferentialRate\": 95.0,\n" +
                            "  \"fixedFee\": 0.0,\n" +
                            "  \"feeRate\": 0.0,\n" +
                            "  \"minAmount\": 100.0,\n" +
                            "  \"maxAmount\": 10000.0,\n" +
                            "  \"isOnlineAvailable\": true,\n" +
                            "  \"isActive\": true,\n" +
                            "  \"description\": \"WiBee뱅킹 최대 95% 우대율로 상향 조정\",\n" +
                            "  \"displayOrder\": 1,\n" +
                            "  \"updatedAt\": \"2025-09-20T15:30:00\"\n" +
                            "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "해당 은행을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터 (중복 은행명/코드, 유효성 검증 실패)",
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
        summary = "은행명으로 정보 수정 (더 편리함)",
        description = "은행명을 이용해 해당 은행의 정보를 수정합니다. ID를 모를 때 사용하세요. " +
                      "예: '우리은행', 'KB국민은행' 등의 이름으로 직접 수정 가능합니다. " +
                      "캐시는 자동으로 무효화되어 즉시 반영됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "성공적으로 은행 정보가 수정됨",
            content = @Content(schema = @Schema(implementation = BankExchangeInfoResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "해당 은행명을 찾을 수 없음",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "은행명 없음 오류",
                    value = "{\n" +
                            "  \"success\": false,\n" +
                            "  \"code\": \"RATE_007\",\n" +
                            "  \"message\": \"해당 은행은 지원하지 않습니다. 지원 은행 목록을 확인해주세요.\",\n" +
                            "  \"detail\": \"활성화된 은행 정보를 찾을 수 없습니다: 잘못된은행명\",\n" +
                            "  \"timestamp\": \"2025-09-20T15:30:00\",\n" +
                            "  \"path\": \"/admin/api/bank-info/name/잘못된은행명\"\n" +
                            "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
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
        summary = "캐시 무효화 (DB 직접 수정 후 사용)",
        description = "은행 정보 캐시를 수동으로 무효화합니다. " +
                      "**사용 시나리오**: SQL로 DB를 직접 수정한 후 이 API를 호출하면 " +
                      "서버 재시작 없이도 변경사항이 즉시 반영됩니다. " +
                      "관리자 API를 사용한 수정에서는 자동으로 캐시가 무효화되므로 호출할 필요 없습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "캐시 무효화 성공",
            content = @Content(
                schema = @Schema(type = "string"),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "성공 메시지",
                    value = "은행 정보 캐시가 무효화되었습니다."
                )
            )
        )
    })
    @PostMapping("/cache/evict")
    public ResponseEntity<String> evictCache() {
        bankInfoService.evictAllBankCache();
        return ResponseEntity.ok("은행 정보 캐시가 무효화되었습니다.");
    }
}