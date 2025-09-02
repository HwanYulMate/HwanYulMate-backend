package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.dto.request.BankExchangeInfoRequestDTO;
import com.swyp.api_server.domain.rate.dto.response.BankExchangeInfoResponseDTO;
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
import java.util.stream.Collectors;

/**
 * 은행 환전 정보 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankExchangeInfoServiceImpl implements BankExchangeInfoService {
    
    private final BankExchangeInfoRepository bankRepository;
    
    @Override
    @Cacheable(value = "bankExchangeInfo", key = "'all_active'")
    public List<BankExchangeInfoResponseDTO> getAllActiveBanks() {
        List<BankExchangeInfo> banks = bankRepository.findAllActiveOrderByDisplayOrder();
        return banks.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<BankExchangeInfo> getAllActiveBankEntities() {
        // DTO로 캐시된 데이터를 Entity로 변환
        List<BankExchangeInfoResponseDTO> cachedDtos = getAllActiveBanks();
        return cachedDtos.stream()
            .map(this::convertToEntity)
            .collect(Collectors.toList());
    }
    
    public List<BankExchangeInfo> getAllActiveBankEntitiesWithoutCache() {
        return bankRepository.findAllActiveOrderByDisplayOrder();
    }
    
    @Override
    public BankExchangeInfoResponseDTO getBankInfo(Long id) {
        BankExchangeInfo bank = bankRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "은행 정보를 찾을 수 없습니다."));
        return convertToResponseDTO(bank);
    }
    
    @Override
    @Cacheable(value = "bankExchangeInfo", key = "'name_' + #bankName")
    public BankExchangeInfoResponseDTO getBankInfoByName(String bankName) {
        BankExchangeInfo bank = bankRepository.findByBankNameAndIsActive(bankName, true)
                .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_BANK, 
                    "활성화된 은행 정보를 찾을 수 없습니다: " + bankName));
        return convertToResponseDTO(bank);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "bankExchangeInfo", allEntries = true)
    public BankExchangeInfoResponseDTO createBankInfo(BankExchangeInfoRequestDTO requestDTO) {
        // 중복 검증
        validateDuplicateBankName(requestDTO.getBankName(), null);
        validateDuplicateBankCode(requestDTO.getBankCode(), null);
        
        BankExchangeInfo bank = BankExchangeInfo.builder()
                .bankName(requestDTO.getBankName())
                .bankCode(requestDTO.getBankCode())
                .spreadRate(requestDTO.getSpreadRate())
                .preferentialRate(requestDTO.getPreferentialRate())
                .fixedFee(requestDTO.getFixedFee())
                .feeRate(requestDTO.getFeeRate())
                .minAmount(requestDTO.getMinAmount())
                .maxAmount(requestDTO.getMaxAmount())
                .isOnlineAvailable(requestDTO.getIsOnlineAvailable())
                .description(requestDTO.getDescription())
                .displayOrder(requestDTO.getDisplayOrder())
                .isActive(true)
                .build();
        
        BankExchangeInfo savedBank = bankRepository.save(bank);
        log.info("새 은행 정보 등록 완료: {}", savedBank.getBankName());
        
        return convertToResponseDTO(savedBank);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "bankExchangeInfo", allEntries = true)
    public BankExchangeInfoResponseDTO updateBankInfo(Long id, BankExchangeInfoRequestDTO requestDTO) {
        BankExchangeInfo bank = bankRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "은행 정보를 찾을 수 없습니다."));
        
        // 중복 검증 (자기 자신 제외)
        validateDuplicateBankName(requestDTO.getBankName(), id);
        validateDuplicateBankCode(requestDTO.getBankCode(), id);
        
        bank.updateBankInfo(
                requestDTO.getSpreadRate(),
                requestDTO.getPreferentialRate(),
                requestDTO.getFixedFee(),
                requestDTO.getFeeRate(),
                requestDTO.getMinAmount(),
                requestDTO.getMaxAmount(),
                requestDTO.getIsOnlineAvailable(),
                requestDTO.getDescription()
        );
        
        bank.updateDisplayOrder(requestDTO.getDisplayOrder());
        
        log.info("은행 정보 수정 완료: {}", bank.getBankName());
        
        return convertToResponseDTO(bank);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "bankExchangeInfo", allEntries = true)
    public BankExchangeInfoResponseDTO toggleBankStatus(Long id) {
        BankExchangeInfo bank = bankRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "은행 정보를 찾을 수 없습니다."));
        
        bank.updateActiveStatus(!bank.getIsActive());
        
        log.info("은행 상태 변경: {} - {}", bank.getBankName(), 
                bank.getIsActive() ? "활성화" : "비활성화");
        
        return convertToResponseDTO(bank);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "bankExchangeInfo", allEntries = true)
    public void deleteBankInfo(Long id) {
        BankExchangeInfo bank = bankRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "은행 정보를 찾을 수 없습니다."));
        
        bank.updateActiveStatus(false);
        log.info("은행 정보 비활성화: {}", bank.getBankName());
    }
    
    @Override
    @Cacheable(value = "bankExchangeInfo", key = "'online_available'")
    public List<BankExchangeInfoResponseDTO> getOnlineAvailableBanks() {
        List<BankExchangeInfo> banks = bankRepository.findOnlineAvailableBanks();
        return banks.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Entity → ResponseDTO 변환
     */
    private BankExchangeInfoResponseDTO convertToResponseDTO(BankExchangeInfo bank) {
        return BankExchangeInfoResponseDTO.builder()
                .id(bank.getId())
                .bankName(bank.getBankName())
                .bankCode(bank.getBankCode())
                .spreadRate(bank.getSpreadRate())
                .preferentialRate(bank.getPreferentialRate())
                .fixedFee(bank.getFixedFee())
                .feeRate(bank.getFeeRate())
                .minAmount(bank.getMinAmount())
                .maxAmount(bank.getMaxAmount())
                .isOnlineAvailable(bank.getIsOnlineAvailable())
                .isActive(bank.getIsActive())
                .description(bank.getDescription())
                .displayOrder(bank.getDisplayOrder())
                .createdAt(bank.getCreatedAt())
                .updatedAt(bank.getUpdatedAt())
                .build();
    }
    
    /**
     * ResponseDTO → Entity 변환 (캐시된 DTO를 Entity로 변환)
     */
    private BankExchangeInfo convertToEntity(BankExchangeInfoResponseDTO dto) {
        return BankExchangeInfo.builder()
                .id(dto.getId())
                .bankName(dto.getBankName())
                .bankCode(dto.getBankCode())
                .spreadRate(dto.getSpreadRate())
                .preferentialRate(dto.getPreferentialRate())
                .fixedFee(dto.getFixedFee())
                .feeRate(dto.getFeeRate())
                .minAmount(dto.getMinAmount())
                .maxAmount(dto.getMaxAmount())
                .isOnlineAvailable(dto.getIsOnlineAvailable())
                .isActive(dto.getIsActive())
                .description(dto.getDescription())
                .displayOrder(dto.getDisplayOrder())
                .build();
    }
    
    /**
     * 은행명 중복 검증
     */
    private void validateDuplicateBankName(String bankName, Long excludeId) {
        if (excludeId == null) {
            if (bankRepository.existsByBankNameAndIdNot(bankName, -1L)) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, 
                    "이미 등록된 은행명입니다: " + bankName);
            }
        } else {
            if (bankRepository.existsByBankNameAndIdNot(bankName, excludeId)) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, 
                    "이미 등록된 은행명입니다: " + bankName);
            }
        }
    }
    
    /**
     * 은행코드 중복 검증
     */
    private void validateDuplicateBankCode(String bankCode, Long excludeId) {
        if (excludeId == null) {
            if (bankRepository.existsByBankCodeAndIdNot(bankCode, -1L)) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, 
                    "이미 등록된 은행코드입니다: " + bankCode);
            }
        } else {
            if (bankRepository.existsByBankCodeAndIdNot(bankCode, excludeId)) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, 
                    "이미 등록된 은행코드입니다: " + bankCode);
            }
        }
    }
}