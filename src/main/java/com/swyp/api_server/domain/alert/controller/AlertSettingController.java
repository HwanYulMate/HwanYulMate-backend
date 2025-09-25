package com.swyp.api_server.domain.alert.controller;

import com.swyp.api_server.domain.alert.dto.AlertSettingRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertTargetRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertTargetResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertDailyRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertDailyResponseDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingResultDTO;
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
                    array = @ArraySchema(schema = @Schema(implementation = AlertSettingResponseDTO.class)),
                    examples = @ExampleObject(
                        name = "알림 설정 목록 예시",
                        value = """
                        [
                          {
                            "currency_code": "USD",
                            "currency_name": "미국 달러",
                            "flag_image_url": "/images/flags/us.png",
                            "is_target_price_enabled": true,
                            "is_daily_alert_enabled": false,
                            "target_price": 1400.00,
                            "target_price_push_how": "ABOVE",
                            "daily_alert_time": "09:00:00"
                          },
                          {
                            "currency_code": "EUR",
                            "currency_name": "유럽 유로",
                            "flag_image_url": "/images/flags/eu.png",
                            "is_target_price_enabled": false,
                            "is_daily_alert_enabled": true,
                            "target_price": null,
                            "target_price_push_how": null,
                            "daily_alert_time": "18:00:00"
                          }
                        ]
                        """
                    )
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
                    array = @ArraySchema(schema = @Schema(implementation = AlertSettingResponseDTO.class)),
                    examples = @ExampleObject(
                        name = "USD 알림 설정 예시 (배열)",
                        value = """
                        [
                          {
                            "currency_code": "USD",
                            "currency_name": "미국 달러",
                            "flag_image_url": "/images/flags/us.png",
                            "is_target_price_enabled": true,
                            "is_daily_alert_enabled": false,
                            "target_price": 1400.00,
                            "target_price_push_how": "ABOVE",
                            "daily_alert_time": "09:00:00"
                          }
                        ]
                        """
                    )
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
    public ResponseEntity<List<AlertSettingResponseDTO>> getAlertSetting(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            HttpServletRequest request) {
        Long userId = authUtil.extractUserId(request);
        AlertSettingResponseDTO alertSetting = alertSettingService.getAlertSetting(userId, currencyCode);
        return ResponseEntity.ok(List.of(alertSetting));
    }
    
    @Operation(
            summary = "목표 환율 알림 설정",
            description = "특정 통화의 목표 환율 알림을 활성화하고 설정합니다. 환율이 목표가격에 도달하면 푸시 알림을 발송합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "목표 환율 알림 설정 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AlertSettingResultDTO.class),
                    examples = @ExampleObject(
                        name = "목표 환율 알림 활성화 성공",
                        value = """
                        {
                          "success": true,
                          "message": "목표 환율 알림이 활성화되었습니다.",
                          "currency_code": "USD",
                          "alert_type": "TARGET",
                          "status": "ENABLED"
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "잘못된 요청 (지원하지 않는 통화 코드, 잘못된 목표 환율 등)",
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
    @PostMapping("/alert/setting/{currencyCode}/target")
    public ResponseEntity<AlertSettingResultDTO> enableTargetAlertSettings(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            @RequestBody(
                description = "목표 환율 알림 설정 정보",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AlertTargetRequestDTO.class),
                    examples = @ExampleObject(
                        name = "목표 환율 알림 설정 예시",
                        value = """
                        {
                          "targetPrice": 1350.7,
                          "condition": "ABOVE"
                        }
                        """
                    )
                )
            )
            @org.springframework.web.bind.annotation.RequestBody AlertTargetRequestDTO targetRequestDTO,
            HttpServletRequest request) {
        
        String userEmail = authUtil.extractUserEmail(request);
        alertSettingService.enableTargetAlertSettings(userEmail, currencyCode, targetRequestDTO);
        
        AlertSettingResultDTO result = AlertSettingResultDTO.builder()
                .success(true)
                .message("목표 환율 알림이 활성화되었습니다.")
                .currencyCode(currencyCode)
                .alertType("TARGET")
                .status("ENABLED")
                .build();
                
        return ResponseEntity.ok(result);
    }
    
    @Operation(
            summary = "목표 환율 알림 비활성화",
            description = "특정 통화의 목표 환율 알림을 비활성화합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "목표 환율 알림 비활성화 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AlertSettingResultDTO.class),
                    examples = @ExampleObject(
                        name = "목표 환율 알림 비활성화 성공",
                        value = """
                        {
                          "success": true,
                          "message": "목표 환율 알림이 비활성화되었습니다.",
                          "currency_code": "USD",
                          "alert_type": "TARGET",
                          "status": "DISABLED"
                        }
                        """
                    )
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
    @DeleteMapping("/alert/setting/{currencyCode}/target")
    public ResponseEntity<AlertSettingResultDTO> disableTargetAlertSettings(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            HttpServletRequest request) {
        
        String userEmail = authUtil.extractUserEmail(request);
        alertSettingService.disableTargetAlertSettings(userEmail, currencyCode);
        
        AlertSettingResultDTO result = AlertSettingResultDTO.builder()
                .success(true)
                .message("목표 환율 알림이 비활성화되었습니다.")
                .currencyCode(currencyCode)
                .alertType("TARGET")
                .status("DISABLED")
                .build();
                
        return ResponseEntity.ok(result);
    }
    
    @Operation(
            summary = "일일 환율 알림 설정",
            description = "특정 통화의 일일 환율 알림을 활성화하고 설정합니다. 매일 지정된 시간에 현재 환율 정보를 푸시 알림으로 발송합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "일일 환율 알림 설정 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AlertSettingResultDTO.class),
                    examples = @ExampleObject(
                        name = "일일 환율 알림 활성화 성공",
                        value = """
                        {
                          "success": true,
                          "message": "일일 환율 알림이 활성화되었습니다.",
                          "currency_code": "USD",
                          "alert_type": "DAILY",
                          "status": "ENABLED"
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "잘못된 요청 (지원하지 않는 통화 코드, 잘못된 시간 형식 등)",
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
    @PostMapping("/alert/setting/{currencyCode}/daily")
    public ResponseEntity<AlertSettingResultDTO> enableDailyAlertSettings(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            @RequestBody(
                description = "일일 환율 알림 설정 정보",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AlertDailyRequestDTO.class),
                    examples = @ExampleObject(
                        name = "일일 환율 알림 설정 예시",
                        value = """
                        {
                          "alertTime": "09:00"
                        }
                        """
                    )
                )
            )
            @org.springframework.web.bind.annotation.RequestBody AlertDailyRequestDTO dailyRequestDTO,
            HttpServletRequest request) {
        
        String userEmail = authUtil.extractUserEmail(request);
        alertSettingService.enableDailyAlertSettings(userEmail, currencyCode, dailyRequestDTO);
        
        AlertSettingResultDTO result = AlertSettingResultDTO.builder()
                .success(true)
                .message("일일 환율 알림이 활성화되었습니다.")
                .currencyCode(currencyCode)
                .alertType("DAILY")
                .status("ENABLED")
                .build();
                
        return ResponseEntity.ok(result);
    }
    
    @Operation(
            summary = "일일 환율 알림 비활성화",
            description = "특정 통화의 일일 환율 알림을 비활성화합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "일일 환율 알림 비활성화 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AlertSettingResultDTO.class),
                    examples = @ExampleObject(
                        name = "일일 환율 알림 비활성화 성공",
                        value = """
                        {
                          "success": true,
                          "message": "일일 환율 알림이 비활성화되었습니다.",
                          "currency_code": "USD",
                          "alert_type": "DAILY",
                          "status": "DISABLED"
                        }
                        """
                    )
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
    @DeleteMapping("/alert/setting/{currencyCode}/daily")
    public ResponseEntity<AlertSettingResultDTO> disableDailyAlertSettings(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            HttpServletRequest request) {
        
        String userEmail = authUtil.extractUserEmail(request);
        alertSettingService.disableDailyAlertSettings(userEmail, currencyCode);
        
        AlertSettingResultDTO result = AlertSettingResultDTO.builder()
                .success(true)
                .message("일일 환율 알림이 비활성화되었습니다.")
                .currencyCode(currencyCode)
                .alertType("DAILY")
                .status("DISABLED")
                .build();
                
        return ResponseEntity.ok(result);
    }
    
    @Operation(
            summary = "목표 환율 알림 설정 조회",
            description = "특정 통화의 목표 환율 알림 설정을 조회합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "목표 환율 알림 설정 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AlertTargetResponseDTO.class),
                    examples = @ExampleObject(
                        name = "목표 환율 알림 설정 조회 예시",
                        value = """
                        {
                          "currency_code": "USD",
                          "currency_name": "미국 달러",
                          "flag_image_url": "/images/flags/us.png",
                          "is_enabled": true,
                          "target_price": 1350.70,
                          "condition": "ABOVE"
                        }
                        """
                    )
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
    @GetMapping("/alert/setting/{currencyCode}/target")
    public ResponseEntity<AlertTargetResponseDTO> getTargetAlertSetting(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            HttpServletRequest request) {
        Long userId = authUtil.extractUserId(request);
        AlertTargetResponseDTO targetSetting = alertSettingService.getTargetAlertSetting(userId, currencyCode);
        return ResponseEntity.ok(targetSetting);
    }
    
    @Operation(
            summary = "일일 환율 알림 설정 조회",
            description = "특정 통화의 일일 환율 알림 설정을 조회합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                responseCode = "200", 
                description = "일일 환율 알림 설정 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AlertDailyResponseDTO.class),
                    examples = @ExampleObject(
                        name = "일일 환율 알림 설정 조회 예시",
                        value = """
                        {
                          "currency_code": "USD",
                          "currency_name": "미국 달러",
                          "flag_image_url": "/images/flags/us.png",
                          "is_enabled": true,
                          "alert_time": "09:00:00"
                        }
                        """
                    )
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
    @GetMapping("/alert/setting/{currencyCode}/daily")
    public ResponseEntity<AlertDailyResponseDTO> getDailyAlertSetting(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            HttpServletRequest request) {
        Long userId = authUtil.extractUserId(request);
        AlertDailyResponseDTO dailySetting = alertSettingService.getDailyAlertSetting(userId, currencyCode);
        return ResponseEntity.ok(dailySetting);
    }
    
    @Operation(
            summary = "[테스트] 일일 환율 알림 즉시 발송",
            description = "디버깅용: 특정 통화의 일일 환율 알림을 즉시 발송합니다. (중복 방지 무시)"
    )
    @PostMapping("/alert/test/daily/{currencyCode}")
    public ResponseEntity<String> testSendDailyAlert(
            @Parameter(description = "통화 코드", example = "USD", required = true)
            @PathVariable String currencyCode,
            HttpServletRequest request) {
        Long userId = authUtil.extractUserId(request);
        boolean success = alertSettingService.testSendDailyAlert(userId, currencyCode);
        
        if (success) {
            return ResponseEntity.ok("일일 환율 알림 테스트 발송 성공");
        } else {
            return ResponseEntity.badRequest().body("일일 환율 알림 테스트 발송 실패");
        }
    }
}
