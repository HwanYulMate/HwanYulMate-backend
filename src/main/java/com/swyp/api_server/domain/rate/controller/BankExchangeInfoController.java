package com.swyp.api_server.domain.rate.controller;

/*
 * TODO: 아직 개발하지 않을 기능으로 전체 클래스 주석 처리
 * 
 * 은행 정보 관리 API (관리자)
 * - GET /admin/api/bank-info (활성화된 은행 정보 목록 조회)
 * - POST /admin/api/bank-info (새 은행 정보 등록)
 * - GET /admin/api/bank-info/{id} (특정 은행 정보 조회)
 * - PUT /admin/api/bank-info/{id} (은행 정보 수정)
 * - DELETE /admin/api/bank-info/{id} (은행 정보 삭제)
 * - PATCH /admin/api/bank-info/{id}/toggle-status (은행 활성화/비활성화)
 * - GET /admin/api/bank-info/online-available (온라인 환전 가능 은행 목록)
 * - GET /admin/api/bank-info/search (은행명으로 조회)
 * 
 * 향후 개발 시 이 주석을 해제하고 사용하세요.
 */

/*
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

@Tag(name = "은행 정보 관리 API (관리자)", description = "은행별 환율 정보 관리 (관리자 전용)")
@RestController
@RequestMapping("/admin/api/bank-info")
@RequiredArgsConstructor
public class BankExchangeInfoController {
    
    private final BankExchangeInfoService bankInfoService;
    
    // 모든 API 메서드들이 여기에 구현됨...
    // 개발할 때 주석 해제하고 사용
}
*/