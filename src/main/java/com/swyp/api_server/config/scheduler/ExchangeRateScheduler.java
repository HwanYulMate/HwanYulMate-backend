package com.swyp.api_server.config.scheduler;

import com.swyp.api_server.domain.rate.service.ExchangeRateHistoryService;
import com.swyp.api_server.domain.rate.service.ExchangeRateService;
import com.swyp.api_server.domain.rate.service.ExchangeRateServiceImpl;
import com.swyp.api_server.domain.rate.service.ExchangeRateStorageService;
import com.swyp.api_server.domain.rate.service.ExchangeRateHistoryInitService;
import com.swyp.api_server.domain.rate.service.ServiceStartTracker;
import com.swyp.api_server.domain.rate.dto.response.ExchangeResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 환율 관련 스케줄러
 * - 매일 환율 히스토리 저장
 * - 환율 데이터 갱신
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateHistoryService historyService;
    private final ExchangeRateStorageService storageService;
    private final ExchangeRateHistoryInitService initService;
    private final ServiceStartTracker serviceStartTracker;

    /**
     * 평일 오전 9시 30분에 환율 데이터 갱신 및 히스토리 저장
     * - 한국수출입은행 API 호출 후 최신 환율 업데이트
     * - 현재 환율을 히스토리 테이블에 저장 (변동률 계산용)
     * 
     * 크론 표현식: "초 분 시 일 월 요일"
     * 0 30 9 * * MON-FRI : 월~금 오전 9시 30분 (환율 고시 후)
     */
    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "Asia/Seoul")
    public void morningExchangeRateUpdate() {
        log.info("========== [오전] 환율 데이터 갱신 및 히스토리 저장 시작 ==========");
        updateExchangeRatesInternal("오전");
    }

    /**
     * 평일 오후 3시에 환율 데이터 추가 갱신
     * - 오후 환율 변동 반영 (필요시)
     * - 데이터 신뢰성 확보를 위한 2차 업데이트
     * 
     * 크론 표현식: 0 0 15 * * MON-FRI : 월~금 오후 3시
     */
    @Scheduled(cron = "0 0 15 * * MON-FRI", zone = "Asia/Seoul")
    public void afternoonExchangeRateUpdate() {
        log.info("========== [오후] 환율 데이터 갱신 및 히스토리 저장 시작 ==========");
        updateExchangeRatesInternal("오후");
    }

    /**
     * 환율 데이터 갱신 공통 로직
     */
    private void updateExchangeRatesInternal(String schedule) {
        try {
            // 1. 최신 환율 데이터 갱신 (한국수출입은행 API 호출 - 스케줄러 전용)
            log.info("1. [{}] 수출입은행 API 직접 호출 중...", schedule);
            ExchangeRateServiceImpl serviceImpl = (ExchangeRateServiceImpl) exchangeRateService;
            List<ExchangeResponseDTO> latestRates = serviceImpl.getExchangeRatesFromKoreaEximForScheduler();
            log.info("✓ [{}] API에서 환율 데이터 수집 완료: {}개 통화", schedule, latestRates.size());

            // 2. 환율 데이터를 DB에 저장
            log.info("2. [{}] 환율 데이터 DB 저장 중...", schedule);
            storageService.saveExchangeRates(latestRates, "KOREA_EXIM");
            log.info("✓ [{}] 환율 데이터 DB 저장 완료", schedule);

            // 3. 현재 환율을 히스토리 테이블에 저장
            log.info("3. [{}] 환율 히스토리 저장 중...", schedule);
            historyService.saveCurrentRatesAsHistory();
            log.info("✓ [{}] 환율 히스토리 저장 완료", schedule);

            log.info("========== [{}] 환율 데이터 갱신 및 히스토리 저장 완료 ==========", schedule);
            
        } catch (Exception e) {
            log.error("[{}] 환율 데이터 갱신 중 오류 발생", schedule, e);
            // TODO: 알림 시스템 연동 (슬랙, 이메일 등)
            // 스케줄러 실패 시 담당자에게 즉시 알림
        }
    }

    /**
     * 매시간 정각에 환율 데이터 갱신 (실시간성 확보) - API 한도 절약을 위해 비활성화
     * - 장중에는 자주 갱신하여 실시간성 확보
     * 
     * 크론 표현식: 0 0 * * * * : 매시간 정각
     */
    // @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul") // API 한도 절약을 위해 비활성화
    public void hourlyUpdateExchangeRates() {
        log.info("시간별 환율 데이터 갱신 시작");
        
        try {
            List<ExchangeResponseDTO> latestRates = exchangeRateService.getAllExchangeRates();
            if (!latestRates.isEmpty()) {
                storageService.saveExchangeRates(latestRates, "KOREA_EXIM");
                log.info("✓ 시간별 환율 데이터 갱신 및 저장 완료: {}개 통화", latestRates.size());
            }
            
        } catch (Exception e) {
            log.warn("시간별 환율 데이터 갱신 중 오류 발생: {}", e.getMessage());
            // 시간별 갱신 실패는 경고 레벨로 처리 (치명적이지 않음)
        }
    }

    /**
     * 매일 오후 6시에 환율 히스토리 정리 (선택사항)
     * - 오래된 히스토리 데이터 정리
     * - 90일 이상 된 데이터 삭제 (DB 용량 관리)
     */
    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "Asia/Seoul")
    public void cleanupOldHistory() {
        log.info("========== 오래된 환율 히스토리 정리 시작 ==========");
        
        try {
            // 90일 이상 된 히스토리 데이터 삭제
            int deletedCount = historyService.deleteOldHistory(90);
            
            if (deletedCount > 0) {
                log.info("✓ 환율 히스토리 정리 완료: {} 건 삭제", deletedCount);
            } else {
                log.info("✓ 삭제할 오래된 데이터가 없어 히스토리 정리를 건너뜀");
            }
            
            log.info("========== 환율 히스토리 정리 완료 ==========");
            
        } catch (Exception e) {
            log.error("환율 히스토리 정리 중 오류 발생", e);
            // TODO: 알림 시스템 연동 (슬랙, 이메일 등)
            // 히스토리 정리 실패 시 담당자에게 알림
        }
    }

    /**
     * 서비스 시작 후 7일차에 히스토리 데이터를 90일로 확장
     * - 매일 오전 10시에 체크하여 조건 만족 시 실행
     */
    @Scheduled(cron = "0 0 10 * * MON-FRI", zone = "Asia/Seoul")
    public void autoExpandTo90Days() {
        try {
            // ServiceStartTracker로 확장 조건 체크
            if (shouldExpandTo90Days()) {
                log.info("========== 자동 히스토리 확장 시작 (90일) ==========");
                initService.expandHistoricalData(90);
                log.info("========== 자동 히스토리 확장 완료 (90일) ==========");
            }
        } catch (Exception e) {
            log.error("90일 히스토리 자동 확장 중 오류 발생", e);
        }
    }

    /**
     * 서비스 시작 후 30일차에 히스토리 데이터를 180일로 확장
     */
    @Scheduled(cron = "0 0 10 * * MON-FRI", zone = "Asia/Seoul")
    public void autoExpandTo180Days() {
        try {
            if (shouldExpandTo180Days()) {
                log.info("========== 자동 히스토리 확장 시작 (180일) ==========");
                initService.expandHistoricalData(180);
                log.info("========== 자동 히스토리 확장 완료 (180일) ==========");
            }
        } catch (Exception e) {
            log.error("180일 히스토리 자동 확장 중 오류 발생", e);
        }
    }

    /**
     * 서비스 시작 후 90일차에 히스토리 데이터를 365일로 확장
     */
    @Scheduled(cron = "0 0 10 * * MON-FRI", zone = "Asia/Seoul")
    public void autoExpandTo365Days() {
        try {
            if (shouldExpandTo365Days()) {
                log.info("========== 자동 히스토리 확장 시작 (365일) ==========");
                initService.expandHistoricalData(365);
                log.info("========== 자동 히스토리 확장 완료 (365일) ==========");
            }
        } catch (Exception e) {
            log.error("365일 히스토리 자동 확장 중 오류 발생", e);
        }
    }

    /**
     * 매일 자동 초기화 체크 (서비스 시작 시 30일 데이터 확보)
     */
    @Scheduled(cron = "0 30 8 * * MON-FRI", zone = "Asia/Seoul")
    public void autoInitializeIfNeeded() {
        try {
            if (initService.needsInitialization()) {
                log.info("========== 자동 초기화 시작 (30일) ==========");
                initService.initializeHistoricalData();
                log.info("========== 자동 초기화 완료 (30일) ==========");
            }
        } catch (Exception e) {
            log.error("자동 초기화 중 오류 발생", e);
        }
    }

    // 확장 조건 체크 메서드들 (ServiceStartTracker 활용)
    private boolean shouldExpandTo90Days() {
        return serviceStartTracker.shouldExpandTo90Days();
    }

    private boolean shouldExpandTo180Days() {
        return serviceStartTracker.shouldExpandTo180Days();
    }

    private boolean shouldExpandTo365Days() {
        return serviceStartTracker.shouldExpandTo365Days();
    }
}