package com.swyp.api_server.domain.rate.repository;

import com.swyp.api_server.entity.BankExchangeInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 은행 환전 정보 Repository
 */
@Repository
public interface BankExchangeInfoRepository extends JpaRepository<BankExchangeInfo, Long> {
    
    /**
     * 활성화된 은행 목록 조회 (표시 순서별 정렬)
     */
    @Query("SELECT b FROM BankExchangeInfo b WHERE b.isActive = true ORDER BY b.displayOrder ASC, b.bankName ASC")
    List<BankExchangeInfo> findAllActiveOrderByDisplayOrder();
    
    /**
     * 은행명으로 조회
     */
    Optional<BankExchangeInfo> findByBankNameAndIsActive(String bankName, Boolean isActive);
    
    /**
     * 은행코드로 조회
     */
    Optional<BankExchangeInfo> findByBankCodeAndIsActive(String bankCode, Boolean isActive);
    
    /**
     * 온라인 환전 가능한 은행 목록 조회
     */
    @Query("SELECT b FROM BankExchangeInfo b WHERE b.isActive = true AND b.isOnlineAvailable = true ORDER BY b.displayOrder ASC")
    List<BankExchangeInfo> findOnlineAvailableBanks();
    
    /**
     * 특정 우대율 이상 은행 목록 조회
     */
    @Query("SELECT b FROM BankExchangeInfo b WHERE b.isActive = true AND b.preferentialRate >= :minRate ORDER BY b.preferentialRate DESC")
    List<BankExchangeInfo> findByMinPreferentialRate(@Param("minRate") java.math.BigDecimal minRate);
    
    /**
     * 은행명 존재 여부 확인
     */
    boolean existsByBankNameAndIdNot(String bankName, Long id);
    
    /**
     * 은행코드 존재 여부 확인  
     */
    boolean existsByBankCodeAndIdNot(String bankCode, Long id);
}