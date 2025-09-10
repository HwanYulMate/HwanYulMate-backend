package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.common.constants.Constants;
import com.swyp.api_server.domain.rate.dto.request.BankExchangeInfoRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.BankExchangeInfoResponseDTO;
import com.swyp.api_server.domain.rate.mapper.BankExchangeInfoMapper;
import com.swyp.api_server.domain.rate.repository.BankExchangeInfoRepository;
import com.swyp.api_server.entity.BankExchangeInfo;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 은행 환전 정보 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankExchangeInfoServiceImpl implements BankExchangeInfoService {
    
    private final BankExchangeInfoRepository bankRepository;
    private final BankExchangeInfoMapper bankMapper;
    
    @Override
    @Cacheable(value = Constants.Cache.BANK_EXCHANGE_INFO, key = "'all_active'")
    public List<BankExchangeInfoResponseDTO> getAllActiveBanks() {
        List<BankExchangeInfo> banks = bankRepository.findAllActiveOrderByDisplayOrder();
        return bankMapper.toResponseDTOs(banks);
    }
    
    @Override
    public List<BankExchangeInfo> getAllActiveBankEntities() {
        // DTO로 캐시된 데이터를 Entity로 변환
        List<BankExchangeInfoResponseDTO> cachedDtos = getAllActiveBanks();
        return bankMapper.toEntities(cachedDtos);
    }
    
    public List<BankExchangeInfo> getAllActiveBankEntitiesWithoutCache() {
        return bankRepository.findAllActiveOrderByDisplayOrder();
    }
    
    @Override
    public BankExchangeInfoResponseDTO getBankInfo(Long id) {
        BankExchangeInfo bank = findBankById(id);
        return bankMapper.toResponseDTO(bank);
    }
    
    @Override
    @Cacheable(value = Constants.Cache.BANK_EXCHANGE_INFO, key = "'name_' + #bankName")
    public BankExchangeInfoResponseDTO getBankInfoByName(String bankName) {
        BankExchangeInfo bank = findActiveBankByName(bankName);
        return bankMapper.toResponseDTO(bank);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = Constants.Cache.BANK_EXCHANGE_INFO, allEntries = true)
    public BankExchangeInfoResponseDTO createBankInfo(BankExchangeInfoRequestDTO requestDTO) {
        // 중복 검증
        validateDuplicateBankName(requestDTO.getBankName(), null);
        validateDuplicateBankCode(requestDTO.getBankCode(), null);
        
        BankExchangeInfo bank = bankMapper.toEntity(requestDTO);
        BankExchangeInfo savedBank = bankRepository.save(bank);
        
        log.info("새 은행 정보 등록 완료: {}", savedBank.getBankName());
        return bankMapper.toResponseDTO(savedBank);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = Constants.Cache.BANK_EXCHANGE_INFO, allEntries = true)
    public BankExchangeInfoResponseDTO updateBankInfo(Long id, BankExchangeInfoRequestDTO requestDTO) {
        BankExchangeInfo bank = findBankById(id);
        
        // 중복 검증 (자기 자신 제외)
        validateDuplicateBankName(requestDTO.getBankName(), id);
        validateDuplicateBankCode(requestDTO.getBankCode(), id);
        
        // MapStruct를 사용한 업데이트
        bankMapper.updateEntityFromRequestDTO(requestDTO, bank);
        
        log.info("은행 정보 수정 완료: {}", bank.getBankName());
        return bankMapper.toResponseDTO(bank);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = Constants.Cache.BANK_EXCHANGE_INFO, allEntries = true)
    public BankExchangeInfoResponseDTO toggleBankStatus(Long id) {
        BankExchangeInfo bank = findBankById(id);
        
        bank.updateActiveStatus(!bank.getIsActive());
        
        log.info("은행 상태 변경: {} - {}", bank.getBankName(), 
                bank.getIsActive() ? "활성화" : "비활성화");
        
        return bankMapper.toResponseDTO(bank);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = Constants.Cache.BANK_EXCHANGE_INFO, allEntries = true)
    public void deleteBankInfo(Long id) {
        BankExchangeInfo bank = findBankById(id);
        
        bank.updateActiveStatus(false);
        log.info("은행 정보 비활성화: {}", bank.getBankName());
    }
    
    @Override
    @Cacheable(value = Constants.Cache.BANK_EXCHANGE_INFO, key = "'online_available'")
    public List<BankExchangeInfoResponseDTO> getOnlineAvailableBanks() {
        List<BankExchangeInfo> banks = bankRepository.findOnlineAvailableBanks();
        return bankMapper.toResponseDTOs(banks);
    }
    
    /**
     * ID로 은행 정보 조회 (공통 메서드)
     */
    private BankExchangeInfo findBankById(Long id) {
        return bankRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("은행 정보를 찾을 수 없습니다: ID={}", id);
                    return new CustomException(ErrorCode.NOT_FOUND, "은행 정보를 찾을 수 없습니다.");
                });
    }
    
    /**
     * 은행명으로 활성 은행 조회 (공통 메서드)
     */
    private BankExchangeInfo findActiveBankByName(String bankName) {
        return bankRepository.findByBankNameAndIsActive(bankName, true)
                .orElseThrow(() -> {
                    log.warn("활성화된 은행 정보를 찾을 수 없습니다: {}", bankName);
                    return new CustomException(ErrorCode.UNSUPPORTED_BANK, 
                        "활성화된 은행 정보를 찾을 수 없습니다: " + bankName);
                });
    }
    
    /**
     * 은행명 중복 검증 (리팩토링된 통합 메서드)
     */
    private void validateDuplicateBankName(String bankName, Long excludeId) {
        Long checkId = excludeId != null ? excludeId : -1L;
        if (bankRepository.existsByBankNameAndIdNot(bankName, checkId)) {
            log.warn("은행명 중복 검증 실패: {}, excludeId={}", bankName, excludeId);
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                "이미 등록된 은행명입니다: " + bankName);
        }
    }
    
    /**
     * 은행코드 중복 검증 (리팩토링된 통합 메서드)
     */
    private void validateDuplicateBankCode(String bankCode, Long excludeId) {
        Long checkId = excludeId != null ? excludeId : -1L;
        if (bankRepository.existsByBankCodeAndIdNot(bankCode, checkId)) {
            log.warn("은행코드 중복 검증 실패: {}, excludeId={}", bankCode, excludeId);
            throw new CustomException(ErrorCode.INVALID_REQUEST, 
                "이미 등록된 은행코드입니다: " + bankCode);
        }
    }
}