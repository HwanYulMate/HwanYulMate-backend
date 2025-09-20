package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import com.swyp.api_server.domain.rate.entity.ExchangeRateHistory;
import com.swyp.api_server.domain.rate.repository.ExchangeRateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 환율 히스토리 초기 데이터 로딩 서비스
 * - 과거 1년치 평일 환율 데이터를 일괄 수집
 * - 서비스 시작 시 완전한 차트 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateHistoryInitService {

    private final ExchangeRateHistoryRepository historyRepository;
    private final ExchangeRateServiceImpl exchangeRateService;

    /**
     * 단계별 환율 히스토리 데이터 초기화 (API 호출량 최적화)
     * - 1단계: 최근 30일 (즉시 차트 기능 활성화)
     * - 2단계: 3개월까지 확장 (일주일 후)
     * - 3단계: 1년까지 확장 (한 달 후)
     */
    @Transactional
    public void initializeHistoricalData() {
        log.info("========== 단계별 환율 히스토리 데이터 초기화 시작 ==========");
        
        try {
            // 기존 데이터 확인
            long existingCount = historyRepository.count();
            if (existingCount > 100) { // 100개 이상이면 이미 초기화된 것으로 간주
                log.info("기존 히스토리 데이터가 충분히 존재합니다 ({} 건). 초기화를 건너뜁니다.", existingCount);
                return;
            }

            // 1단계: 최근 30일 데이터 수집 (우선순위 높음)
            log.info("1단계: 최근 30일 평일 데이터 수집 시작");
            initializeRecentData(30);
            
            log.info("========== 1단계 환율 히스토리 초기화 완료 (차트 기능 활성화) ==========");

        } catch (Exception e) {
            log.error("환율 히스토리 데이터 초기화 중 치명적 오류 발생", e);
            throw new RuntimeException("환율 히스토리 초기화 실패", e);
        }
    }

    /**
     * 확장 초기화 (3개월~1년치 데이터)
     * - 별도 API 호출로 점진적 확장
     * - 일일 API 호출 한도 고려
     * - 중복 확장 방지 로직 포함
     */
    @Transactional
    public void expandHistoricalData(int targetDays) {
        log.info("========== 환율 히스토리 데이터 확장 시작: {} 일 ==========", targetDays);
        
        try {
            // 이미 해당 기간만큼 확장되었는지 확인
            if (isAlreadyExpanded(targetDays)) {
                log.info("이미 {} 일치 데이터로 확장 완료되어 있습니다. 건너뜁니다.", targetDays);
                return;
            }

            // 현재 가장 오래된 데이터 날짜 확인
            LocalDate oldestDate = getOldestHistoryDate();
            if (oldestDate == null) {
                log.warn("기존 히스토리 데이터가 없어 기본 초기화 실행");
                initializeRecentData(30);
                return;
            }

            // 확장 대상 날짜 계산
            LocalDate targetStartDate = LocalDate.now().minusDays(targetDays);
            if (!oldestDate.isAfter(targetStartDate)) {
                log.info("이미 {} 일치 데이터가 존재합니다. 확장이 불필요합니다.", targetDays);
                return;
            }

            // 확장 데이터 수집
            List<LocalDate> expandDates = generateBusinessDaysBetween(targetStartDate, oldestDate.minusDays(1));
            log.info("확장 수집 대상: {} 일 (API 호출: {} 회)", expandDates.size(), expandDates.size());

            if (expandDates.size() > 100) {
                log.warn("확장 대상이 너무 많습니다 ({} 일). 100일로 제한합니다.", expandDates.size());
                expandDates = expandDates.subList(0, Math.min(100, expandDates.size()));
            }

            collectHistoryForDateList(expandDates, "확장");

        } catch (Exception e) {
            log.error("환율 히스토리 확장 중 오류 발생", e);
            throw new RuntimeException("환율 히스토리 확장 실패", e);
        }
    }

    /**
     * 이미 해당 기간만큼 확장되었는지 확인
     */
    private boolean isAlreadyExpanded(int targetDays) {
        LocalDate oldestDate = getOldestHistoryDate();
        if (oldestDate == null) {
            return false;
        }
        
        LocalDate targetStartDate = LocalDate.now().minusDays(targetDays);
        return !oldestDate.isAfter(targetStartDate);
    }

    /**
     * 최근 N일 데이터 초기화
     */
    private void initializeRecentData(int days) {
        List<LocalDate> businessDays = generateRecentBusinessDays(days);
        log.info("최근 {} 일 평일 데이터 수집: {} 일 (API 호출: {} 회)", days, businessDays.size(), businessDays.size());
        collectHistoryForDateList(businessDays, "초기화");
    }

    /**
     * 날짜 리스트에 대한 히스토리 데이터 수집
     */
    private void collectHistoryForDateList(List<LocalDate> dates, String phase) {
        int successCount = 0;
        int failCount = 0;

        for (LocalDate date : dates) {
            try {
                boolean success = collectAndSaveHistoryForDate(date);
                if (success) {
                    successCount++;
                    log.info("[{}] 날짜 {} 환율 데이터 저장 완료 ({}/{})", 
                            phase, date, successCount, dates.size());
                } else {
                    failCount++;
                    log.warn("[{}] 날짜 {} 환율 데이터 수집 실패", phase, date);
                }

                // API 호출 간격 조절 (실서비스 고려)
                Thread.sleep(500); // 500ms 대기 (안전 마진)
                
            } catch (Exception e) {
                failCount++;
                log.error("[{}] 날짜 {} 환율 데이터 처리 중 오류: {}", phase, date, e.getMessage());
                
                // 연속 실패 시 중단
                if (failCount > 5) {
                    log.error("[{}] 연속 실패가 5회를 초과하여 중단합니다", phase);
                    break;
                }
            }
        }

        log.info("[{}] 완료: 성공 {} 일, 실패 {} 일, 전체 {} 일", phase, successCount, failCount, dates.size());
    }

    /**
     * 특정 날짜의 환율 데이터 수집 및 저장
     */
    private boolean collectAndSaveHistoryForDate(LocalDate date) {
        try {
            String dateString = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            log.debug("날짜 {} 환율 데이터 수집 시작", dateString);

            // 특정 날짜의 환율 데이터 수집
            List<ExchangeResponseDTO> dailyRates = exchangeRateService.getExchangeRatesForSpecificDate(dateString);
            
            if (dailyRates.isEmpty()) {
                log.warn("날짜 {} 환율 데이터가 없습니다", dateString);
                return false;
            }

            // 히스토리 엔티티로 변환 및 저장
            List<ExchangeRateHistory> historyList = convertToHistoryEntities(dailyRates, date);
            historyRepository.saveAll(historyList);

            log.debug("날짜 {} 환율 데이터 저장 완료: {} 통화", dateString, historyList.size());
            return true;

        } catch (Exception e) {
            log.error("날짜 {} 환율 데이터 처리 실패: {}", date, e.getMessage());
            return false;
        }
    }

    /**
     * 환율 응답 DTO를 히스토리 엔티티로 변환
     */
    private List<ExchangeRateHistory> convertToHistoryEntities(List<ExchangeResponseDTO> exchangeRates, LocalDate date) {
        List<ExchangeRateHistory> historyList = new ArrayList<>();

        for (ExchangeResponseDTO rate : exchangeRates) {
            // 중복 방지 - 같은 날짜, 같은 통화 데이터가 이미 있는지 확인
            if (!historyRepository.existsByBaseDateAndCurrencyCode(date, rate.getCurrencyCode())) {
                ExchangeRateHistory history = ExchangeRateHistory.builder()
                        .currencyCode(rate.getCurrencyCode())
                        .currencyName(rate.getCurrencyName())
                        .exchangeRate(rate.getExchangeRate())
                        .baseDate(date)
                        .build();
                historyList.add(history);
            } else {
                log.debug("중복 데이터 건너뜀: {} - {}", date, rate.getCurrencyCode());
            }
        }

        return historyList;
    }

    /**
     * 최근 N일간의 평일 날짜 리스트 생성 (최신순)
     */
    private List<LocalDate> generateRecentBusinessDays(int days) {
        List<LocalDate> businessDays = new ArrayList<>();
        LocalDate currentDate = LocalDate.now().minusDays(1); // 어제부터 시작
        
        while (businessDays.size() < days && currentDate.isAfter(LocalDate.now().minusYears(2))) {
            if (isBusinessDay(currentDate)) {
                businessDays.add(currentDate);
            }
            currentDate = currentDate.minusDays(1);
        }
        
        // 오래된 날짜부터 수집하도록 역순 정렬
        businessDays.sort(LocalDate::compareTo);
        
        log.info("최근 {} 일 평일 날짜 생성 완료: {} 일", days, businessDays.size());
        return businessDays;
    }

    /**
     * 두 날짜 사이의 평일 날짜 리스트 생성
     */
    private List<LocalDate> generateBusinessDaysBetween(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> businessDays = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            if (isBusinessDay(currentDate)) {
                businessDays.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }
        
        log.info("기간별 평일 날짜 생성 완료: {} ~ {} ({} 일)", 
                startDate, endDate, businessDays.size());
        return businessDays;
    }

    /**
     * 가장 오래된 히스토리 데이터 날짜 조회
     */
    private LocalDate getOldestHistoryDate() {
        return historyRepository.findOldestBaseDate().orElse(null);
    }

    /**
     * 평일 여부 확인 (토, 일 제외)
     */
    private boolean isBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * 초기화 필요 여부 확인
     */
    public boolean needsInitialization() {
        long count = historyRepository.count();
        log.info("현재 히스토리 데이터 개수: {}", count);
        return count == 0;
    }

    /**
     * 수동 초기화 트리거 (관리자용)
     * - 기존 데이터 삭제 후 재초기화
     */
    @Transactional
    public void forceReinitialize() {
        log.warn("========== 강제 재초기화 시작 - 기존 데이터 삭제 ==========");
        
        try {
            long deletedCount = historyRepository.count();
            historyRepository.deleteAll();
            log.warn("기존 히스토리 데이터 삭제 완료: {} 건", deletedCount);

            initializeHistoricalData();
            
        } catch (Exception e) {
            log.error("강제 재초기화 중 오류 발생", e);
            throw new RuntimeException("강제 재초기화 실패", e);
        }
    }
}