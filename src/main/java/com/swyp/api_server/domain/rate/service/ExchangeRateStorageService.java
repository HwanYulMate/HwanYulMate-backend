package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import com.swyp.api_server.domain.rate.repository.ExchangeRateRepository;
import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.entity.ExchangeRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 환율 데이터 저장 및 조회 서비스
 * - API에서 가져온 환율 데이터를 DB에 저장
 * - API 실패 시 DB에서 최근 데이터 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateStorageService {
    
    private final ExchangeRateRepository exchangeRateRepository;
    
    /**
     * 환율 데이터를 DB에 저장
     */
    @Transactional
    public void saveExchangeRates(List<ExchangeResponseDTO> rates, String sourceApi) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            for (ExchangeResponseDTO rate : rates) {
                // 기존 데이터 확인
                Optional<ExchangeRate> existing = exchangeRateRepository
                        .findByCurrencyCodeAndBaseDate(rate.getCurrencyCode(), today);
                
                if (existing.isPresent()) {
                    // 기존 데이터 업데이트
                    existing.get().updateRate(rate.getExchangeRate());
                    log.debug("환율 데이터 업데이트: {} = {}", rate.getCurrencyCode(), rate.getExchangeRate());
                } else {
                    // 새 데이터 저장
                    ExchangeRate entity = ExchangeRate.builder()
                            .currencyCode(rate.getCurrencyCode())
                            .currencyName(rate.getCurrencyName())
                            .exchangeRate(rate.getExchangeRate())
                            .baseDate(today)
                            .sourceApi(sourceApi)
                            .build();
                    
                    exchangeRateRepository.save(entity);
                    log.debug("새 환율 데이터 저장: {} = {}", rate.getCurrencyCode(), rate.getExchangeRate());
                }
            }
            
            log.info("환율 데이터 저장 완료: {}개 통화, 소스: {}", rates.size(), sourceApi);
            
        } catch (Exception e) {
            log.error("환율 데이터 저장 실패", e);
        }
    }
    
    /**
     * DB에서 최신 환율 데이터 조회
     */
    public List<ExchangeResponseDTO> getLatestRatesFromDB() {
        try {
            List<ExchangeRate> latestRates = exchangeRateRepository.findAllLatestRates();
            
            List<ExchangeResponseDTO> results = latestRates.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            log.info("DB에서 최신 환율 조회: {}개 통화", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("DB 환율 조회 실패", e);
            return List.of();
        }
    }
    
    /**
     * 특정 통화의 최신 환율 조회
     */
    public Optional<ExchangeResponseDTO> getLatestRateForCurrency(String currencyCode) {
        try {
            Optional<ExchangeRate> rate = exchangeRateRepository.findLatestByCurrencyCode(currencyCode);
            return rate.map(this::convertToDTO);
        } catch (Exception e) {
            log.error("DB에서 {} 환율 조회 실패", currencyCode, e);
            return Optional.empty();
        }
    }
    
    /**
     * 특정 날짜의 환율 데이터 조회
     */
    public List<ExchangeResponseDTO> getRatesByDate(String baseDate) {
        try {
            List<ExchangeRate> rates = exchangeRateRepository.findByBaseDateOrderByCurrencyCodeAsc(baseDate);
            
            return rates.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("DB에서 {}일자 환율 조회 실패", baseDate, e);
            return List.of();
        }
    }
    
    /**
     * 오래된 데이터 정리 (30일 이전 데이터 삭제)
     */
    @Transactional
    public void cleanupOldData() {
        try {
            String cutoffDate = LocalDate.now().minusDays(30)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            exchangeRateRepository.deleteByBaseDateBefore(cutoffDate);
            log.info("30일 이전 환율 데이터 정리 완료");
            
        } catch (Exception e) {
            log.error("환율 데이터 정리 실패", e);
        }
    }
    
    /**
     * Entity -> DTO 변환
     */
    private ExchangeResponseDTO convertToDTO(ExchangeRate entity) {
        return ExchangeResponseDTO.builder()
                .currencyCode(entity.getCurrencyCode())
                .currencyName(entity.getCurrencyName())
                .flagImageUrl(getFlagImageUrl(entity.getCurrencyCode()))
                .exchangeRate(entity.getExchangeRate())
                .baseDate(entity.getBaseDate())
                .build();
    }
    
    /**
     * 통화 코드에 해당하는 국기 이미지 URL 조회
     */
    private String getFlagImageUrl(String currencyCode) {
        try {
            return ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase()).getFlagImageUrl();
        } catch (IllegalArgumentException e) {
            return "/images/flags/default.png";
        }
    }
}