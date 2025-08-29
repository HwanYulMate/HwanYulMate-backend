package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.entity.BankExchangeInfo;
import com.swyp.api_server.domain.rate.dto.request.BankExchangeInfoRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.BankExchangeInfoResponseDTO;

import java.util.List;

/**
 * 은행 환전 정보 관리 서비스 인터페이스
 */
public interface BankExchangeInfoService {
    
    /**
     * 활성화된 모든 은행 정보 조회
     */
    List<BankExchangeInfoResponseDTO> getAllActiveBanks();
    
    /**
     * 활성화된 은행 엔티티 목록 조회 (내부 사용)
     */
    List<BankExchangeInfo> getAllActiveBankEntities();
    
    /**
     * 특정 은행 정보 조회
     */
    BankExchangeInfoResponseDTO getBankInfo(Long id);
    
    /**
     * 은행명으로 조회
     */
    BankExchangeInfoResponseDTO getBankInfoByName(String bankName);
    
    /**
     * 새 은행 정보 등록
     */
    BankExchangeInfoResponseDTO createBankInfo(BankExchangeInfoRequestDTO requestDTO);
    
    /**
     * 은행 정보 수정
     */
    BankExchangeInfoResponseDTO updateBankInfo(Long id, BankExchangeInfoRequestDTO requestDTO);
    
    /**
     * 은행 정보 활성화/비활성화
     */
    BankExchangeInfoResponseDTO toggleBankStatus(Long id);
    
    /**
     * 은행 정보 삭제 (비활성화)
     */
    void deleteBankInfo(Long id);
    
    /**
     * 온라인 환전 가능한 은행 목록
     */
    List<BankExchangeInfoResponseDTO> getOnlineAvailableBanks();
}