package com.swyp.api_server.config;

import com.swyp.api_server.domain.rate.repository.BankExchangeInfoRepository;
import com.swyp.api_server.entity.BankExchangeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 초기 은행 데이터 로더
 * - 애플리케이션 시작 시 기본 은행 정보 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    
    private final BankExchangeInfoRepository bankRepository;
    
    @Override
    public void run(String... args) throws Exception {
        loadInitialBankData();
    }
    
    /**
     * 초기 은행 데이터 생성
     */
    private void loadInitialBankData() {
        // 데이터가 이미 있으면 건너뛰기
        if (bankRepository.count() > 0) {
            log.info("은행 데이터가 이미 존재합니다. 초기화 건너뜀");
            return;
        }
        
        log.info("초기 은행 데이터 생성 시작");
        
        List<BankExchangeInfo> initialBanks = Arrays.asList(
            // 우리은행 - WiBee뱅킹 (실제 우대율: 90%)
            BankExchangeInfo.builder()
                    .bankName("우리은행")
                    .bankCode("020")
                    .spreadRate(BigDecimal.valueOf(1.0))
                    .preferentialRate(BigDecimal.valueOf(90))
                    .fixedFee(BigDecimal.valueOf(0))
                    .feeRate(BigDecimal.valueOf(0.0))
                    .minAmount(BigDecimal.valueOf(100))
                    .maxAmount(BigDecimal.valueOf(10000))
                    .isOnlineAvailable(true)
                    .description("WiBee뱅킹 최대 90% 우대율, 수수료 무료")
                    .displayOrder(1)
                    .isActive(true)
                    .build(),
            
            // IM뱅크 (DGB대구은행 디지털뱅킹) - 실제 우대율: 80%
            BankExchangeInfo.builder()
                    .bankName("IM뱅크")
                    .bankCode("031")
                    .spreadRate(BigDecimal.valueOf(0.8))
                    .preferentialRate(BigDecimal.valueOf(80))
                    .fixedFee(BigDecimal.valueOf(1000))
                    .feeRate(BigDecimal.valueOf(0.05))
                    .minAmount(BigDecimal.valueOf(100))
                    .maxAmount(BigDecimal.valueOf(5000))
                    .isOnlineAvailable(true)
                    .description("디지털뱅크 최대 80% 우대율")
                    .displayOrder(2)
                    .isActive(true)
                    .build(),
            
            // KB국민은행 - 실제 우대율: 75%
            BankExchangeInfo.builder()
                    .bankName("KB국민은행")
                    .bankCode("004")
                    .spreadRate(BigDecimal.valueOf(1.2))
                    .preferentialRate(BigDecimal.valueOf(75))
                    .fixedFee(BigDecimal.valueOf(1000))
                    .feeRate(BigDecimal.valueOf(0.1))
                    .minAmount(BigDecimal.valueOf(100))
                    .maxAmount(BigDecimal.valueOf(10000))
                    .isOnlineAvailable(true)
                    .description("KB스타뱅킹 최대 75% 우대율")
                    .displayOrder(3)
                    .isActive(true)
                    .build(),
            
            // 하나은행 - 실제 우대율: 70%
            BankExchangeInfo.builder()
                    .bankName("하나은행")
                    .bankCode("081")
                    .spreadRate(BigDecimal.valueOf(1.0))
                    .preferentialRate(BigDecimal.valueOf(70))
                    .fixedFee(BigDecimal.valueOf(1500))
                    .feeRate(BigDecimal.valueOf(0.1))
                    .minAmount(BigDecimal.valueOf(100))
                    .maxAmount(BigDecimal.valueOf(5000))
                    .isOnlineAvailable(true)
                    .description("하나원큐 최대 70% 우대율")
                    .displayOrder(4)
                    .isActive(true)
                    .build(),
            
            // 신한은행 - 실제 우대율: 60%
            BankExchangeInfo.builder()
                    .bankName("신한은행")
                    .bankCode("088")
                    .spreadRate(BigDecimal.valueOf(1.1))
                    .preferentialRate(BigDecimal.valueOf(60))
                    .fixedFee(BigDecimal.valueOf(2000))
                    .feeRate(BigDecimal.valueOf(0.1))
                    .minAmount(BigDecimal.valueOf(100))
                    .maxAmount(BigDecimal.valueOf(5000))
                    .isOnlineAvailable(true)
                    .description("신한 SOL뱅킹 최대 60% 우대율")
                    .displayOrder(5)
                    .isActive(true)
                    .build(),
            
            // 한국씨티은행 - 실제 우대율: 50%
            BankExchangeInfo.builder()
                    .bankName("한국씨티은행")
                    .bankCode("027")
                    .spreadRate(BigDecimal.valueOf(1.3))
                    .preferentialRate(BigDecimal.valueOf(50))
                    .fixedFee(BigDecimal.valueOf(1800))
                    .feeRate(BigDecimal.valueOf(0.15))
                    .minAmount(BigDecimal.valueOf(500))
                    .maxAmount(BigDecimal.valueOf(5000))
                    .isOnlineAvailable(true)
                    .description("씨티모바일 최대 50% 우대율")
                    .displayOrder(6)
                    .isActive(true)
                    .build(),
                    
            // SC제일은행 - 실제 우대율: 40%
            BankExchangeInfo.builder()
                    .bankName("SC제일은행")
                    .bankCode("023")
                    .spreadRate(BigDecimal.valueOf(1.4))
                    .preferentialRate(BigDecimal.valueOf(40))
                    .fixedFee(BigDecimal.valueOf(2500))
                    .feeRate(BigDecimal.valueOf(0.2))
                    .minAmount(BigDecimal.valueOf(500))
                    .maxAmount(BigDecimal.valueOf(3000))
                    .isOnlineAvailable(true)
                    .description("SC인터넷뱅킹 최대 40% 우대율")
                    .displayOrder(7)
                    .isActive(true)
                    .build()
        );
        
        try {
            List<BankExchangeInfo> savedBanks = bankRepository.saveAll(initialBanks);
            log.info("초기 은행 데이터 생성 완료: {}개 은행 등록", savedBanks.size());
            
            // 생성된 은행 목록 로그 출력
            savedBanks.forEach(bank -> 
                log.info("등록된 은행: {} ({}%, 수수료: {}원)", 
                    bank.getBankName(), 
                    bank.getPreferentialRate(), 
                    bank.getFixedFee())
            );
            
        } catch (Exception e) {
            log.error("초기 은행 데이터 생성 실패", e);
        }
    }
}