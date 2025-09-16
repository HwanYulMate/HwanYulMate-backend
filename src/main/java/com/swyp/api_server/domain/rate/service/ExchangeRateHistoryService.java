package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.dto.ExchangeRateWithChangeDto;
import com.swyp.api_server.entity.ExchangeRate;
import com.swyp.api_server.domain.rate.entity.ExchangeRateHistory;
import com.swyp.api_server.domain.rate.repository.ExchangeRateHistoryRepository;
import com.swyp.api_server.domain.rate.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 환율 히스토리 서비스
 * - 환율 히스토리 저장 및 변동률 계산
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateHistoryService {

    private final ExchangeRateHistoryRepository historyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    /**
     * 현재 환율 데이터를 히스토리에 저장
     */
    @Transactional
    @Async
    public void saveCurrentRatesAsHistory() {
        LocalDate today = LocalDate.now();
        
        // 오늘 데이터가 이미 저장되었는지 확인
        if (historyRepository.existsByBaseDate(today)) {
            log.info("오늘({}) 환율 히스토리가 이미 존재합니다. 저장을 건너뜁니다.", today);
            return;
        }

        List<ExchangeRate> currentRates = exchangeRateRepository.findAllLatestRates();
        if (currentRates.isEmpty()) {
            log.warn("현재 환율 데이터가 없어서 히스토리 저장을 건너뜁니다.");
            return;
        }

        List<ExchangeRateHistory> histories = currentRates.stream()
                .map(rate -> ExchangeRateHistory.from(rate, today))
                .collect(Collectors.toList());

        historyRepository.saveAll(histories);
        log.info("환율 히스토리 저장 완료: {} 건 (기준일: {})", histories.size(), today);
    }

    /**
     * 변동률이 포함된 환율 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ExchangeRateWithChangeDto> getRatesWithChange() {
        List<ExchangeRate> currentRates = exchangeRateRepository.findAllLatestRates();
        LocalDate today = LocalDate.now();
        
        // 전일 환율 데이터 조회
        Map<String, ExchangeRateHistory> previousRatesMap = getPreviousRatesMap(today);
        
        List<ExchangeRateWithChangeDto> result = new ArrayList<>();
        for (ExchangeRate current : currentRates) {
            ExchangeRateHistory previous = previousRatesMap.get(current.getCurrencyCode());
            result.add(ExchangeRateWithChangeDto.of(current, previous));
        }
        
        log.info("변동률 포함 환율 데이터 조회 완료: {} 건", result.size());
        return result;
    }

    /**
     * 특정 통화의 변동률 포함 환율 조회
     */
    @Transactional(readOnly = true)
    public ExchangeRateWithChangeDto getRateWithChange(String currencyCode) {
        Optional<ExchangeRate> currentOpt = exchangeRateRepository.findLatestByCurrencyCode(currencyCode);
        if (currentOpt.isEmpty()) {
            return null;
        }

        ExchangeRate current = currentOpt.get();
        LocalDate today = LocalDate.now();
        
        Optional<ExchangeRateHistory> previousOpt = historyRepository.findPreviousDayRate(currencyCode, today);
        
        return ExchangeRateWithChangeDto.of(current, previousOpt.orElse(null));
    }

    /**
     * 전일 환율 데이터 Map 조회
     */
    private Map<String, ExchangeRateHistory> getPreviousRatesMap(LocalDate currentDate) {
        // 전일부터 최대 7일 전까지 조회 (주말, 공휴일 고려)
        LocalDate searchDate = currentDate.minusDays(1);
        for (int i = 0; i < 7; i++) {
            List<ExchangeRateHistory> previousRates = historyRepository.findAllByPreviousDate(searchDate);
            if (!previousRates.isEmpty()) {
                log.info("전일 환율 데이터 조회 성공: {} ({} 건)", searchDate, previousRates.size());
                return previousRates.stream()
                        .collect(Collectors.toMap(
                                ExchangeRateHistory::getCurrencyCode,
                                history -> history
                        ));
            }
            searchDate = searchDate.minusDays(1);
        }
        
        log.warn("전일 환율 데이터를 찾을 수 없습니다. 변동률은 0으로 표시됩니다.");
        return Map.of();
    }

    /**
     * 특정 기간의 환율 히스토리 조회 (차트용)
     */
    @Transactional(readOnly = true)
    public List<ExchangeRateHistory> getHistoryByPeriod(String currencyCode, LocalDate startDate, LocalDate endDate) {
        return historyRepository.findByPeriod(currencyCode, startDate, endDate);
    }

    /**
     * 특정 통화의 최근 N일 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<ExchangeRateHistory> getRecentHistory(String currencyCode, int days) {
        return historyRepository.findRecentHistoryByCurrency(currencyCode, days);
    }
    
    /**
     * 90일 이상 된 오래된 환율 히스토리 데이터 삭제
     * @param retentionDays 보관할 일수 (기본 90일)
     * @return 삭제된 데이터 건수
     */
    @Transactional
    public int deleteOldHistory(int retentionDays) {
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        
        log.info("환율 히스토리 정리 시작: {}일 이전 데이터 삭제 (기준일: {})", retentionDays, cutoffDate);
        
        try {
            // 삭제 전 카운트 조회
            List<ExchangeRateHistory> oldHistories = historyRepository.findByBaseDateBefore(cutoffDate);
            int deleteCount = oldHistories.size();
            
            if (deleteCount == 0) {
                log.info("삭제할 오래된 히스토리 데이터가 없습니다.");
                return 0;
            }
            
            // 삭제 실행
            historyRepository.deleteByBaseDateBefore(cutoffDate);
            
            log.info("환율 히스토리 정리 완료: {} 건 삭제 ({}일 이전 데이터)", deleteCount, retentionDays);
            return deleteCount;
            
        } catch (Exception e) {
            log.error("환율 히스토리 정리 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * 90일 이상 된 오래된 환율 히스토리 데이터 삭제 (기본값)
     */
    @Transactional
    public int deleteOldHistory() {
        return deleteOldHistory(90);
    }
}