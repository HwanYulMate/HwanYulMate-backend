package com.swyp.api_server.domain.alert.controller;

import com.swyp.api_server.domain.alert.dto.AlertSettingRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingDetailRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingResponseDTO;
import com.swyp.api_server.domain.alert.service.AlertSettingService;
import com.swyp.api_server.common.util.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.swyp.api_server.common.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Alert Settings", description = "알림 설정 API")
public class AlertSettingController {
    
    private final AlertSettingService alertSettingService;
    private final AuthUtil authUtil;

    @Operation(
            summary = "통화별 알림 설정 저장",
            description = "사용자의 통화별 알림 활성화/비활성화 설정을 저장합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "알림 설정 저장 성공",
                content = @Content(examples = @ExampleObject(value = "알림 설정이 저장되었습니다."))
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "잘못된 요청 (유효하지 않은 통화 코드, 잘못된 알림 설정 데이터 등)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 필요 (유효하지 않은 토큰 등)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "사용자를 찾을 수 없습니다",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버에서 예상치 못한 오류가 발생했습니다",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/alert/setting")
    public ResponseEntity<String> saveAlertSettings(
            @org.springframework.web.bind.annotation.RequestBody List<AlertSettingRequestDTO> alertSettingRequestDTO,
            HttpServletRequest request) {
        
        String userEmail = authUtil.extractUserEmail(request);
        
        alertSettingService.saveAlertSettings(userEmail, alertSettingRequestDTO);
        return ResponseEntity.ok("알림 설정이 저장되었습니다.");
    }


    @Operation(
            summary = "알림 상세 설정 저장", 
            description = "특정 통화의 목표 환율 및 오늘의 환율 알림 상세 설정을 저장합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "알림 상세 설정 저장 성공",
                content = @Content(examples = @ExampleObject(value = "알림 상세 설정이 저장되었습니다."))
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "잘못된 요청 (지원하지 않는 통화 코드, 잘못된 알림 상세 설정 데이터 등)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 필요 (유효하지 않은 토큰 등)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "사용자를 찾을 수 없습니다",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버에서 예상치 못한 오류가 발생했습니다",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/alert/setting/{currencyCode}/detail")
    public ResponseEntity<String> saveDetailAlertSettings(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            @org.springframework.web.bind.annotation.RequestBody AlertSettingDetailRequestDTO detailRequestDTO,
            HttpServletRequest request) {
        
        String userEmail = authUtil.extractUserEmail(request);
        
        alertSettingService.saveDetailAlertSettings(userEmail, currencyCode, detailRequestDTO);
        return ResponseEntity.ok("알림 상세 설정이 저장되었습니다.");
    }

    @Operation(
            summary = "사용자 전체 알림 설정 조회",
            description = "사용자의 모든 활성화된 알림 설정을 조회합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "알림 설정 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AlertSettingResponseDTO.class))
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 필요 (유효하지 않은 토큰 등)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "사용자를 찾을 수 없습니다",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버에서 예상치 못한 오류가 발생했습니다",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/alert/settings")
    public ResponseEntity<List<AlertSettingResponseDTO>> getAllAlertSettings(HttpServletRequest request) {
        Long userId = authUtil.extractUserId(request);
        List<AlertSettingResponseDTO> alertSettings = alertSettingService.getAllAlertSettings(userId);
        return ResponseEntity.ok(alertSettings);
    }

    @Operation(
            summary = "특정 통화 알림 설정 조회",
            description = "특정 통화에 대한 알림 설정을 조회합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "알림 설정 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AlertSettingResponseDTO.class)
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 필요 (유효하지 않은 토큰 등)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "사용자 또는 알림 설정을 찾을 수 없습니다",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버에서 예상치 못한 오류가 발생했습니다",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/alert/setting/{currencyCode}")
    public ResponseEntity<AlertSettingResponseDTO> getAlertSetting(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            HttpServletRequest request) {
        Long userId = authUtil.extractUserId(request);
        AlertSettingResponseDTO alertSetting = alertSettingService.getAlertSetting(userId, currencyCode);
        return ResponseEntity.ok(alertSetting);
    }
}
