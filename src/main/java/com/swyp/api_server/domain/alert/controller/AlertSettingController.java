package com.swyp.api_server.domain.alert.controller;

import com.swyp.api_server.domain.alert.dto.AlertSettingRequestDTO;
import com.swyp.api_server.domain.alert.dto.AlertSettingDetailRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/v1/api")
@Tag(name = "Alert Settings", description = "알림 설정 API")
public class AlertSettingController {

    @Operation(
            summary = "알림 설정 조회/미리보기",
            description = "클라이언트에서 구성한 알림 설정 목록을 전달받아 유효성 검사 후 상태 코드를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공 – 콘텐츠 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @RequestBody(
            description = "알림 설정 요청 DTO 리스트",
            required = true,
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = AlertSettingRequestDTO.class))
            )
    )
    @PostMapping("/alert/setting")
    public ResponseEntity<Void> alertSetting(List<AlertSettingRequestDTO> alertSettingRequestDTO) {
        return ResponseEntity.status(204).build();
    }


    @Operation(
            summary = "알림 상세 설정 조회/미리보기",
            description = "상세 알림 설정 목록을 전달받아 유효성 검사 후 상태 코드를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공 – 콘텐츠 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @RequestBody(
            description = "알림 상세 설정 요청 DTO 리스트",
            required = true,
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = AlertSettingDetailRequestDTO.class))
            )
    )
    @PostMapping("/alert/setting/detail")
    public ResponseEntity<Void> alertSettingDetail(List<AlertSettingRequestDTO> alertSettingRequestDTO) {
        return ResponseEntity.status(204).build();
    }
}
