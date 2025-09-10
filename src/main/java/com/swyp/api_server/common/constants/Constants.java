package com.swyp.api_server.common.constants;

/**
 * 애플리케이션 전체에서 사용되는 상수 정의
 */
public final class Constants {
    
    private Constants() {
        // 유틸리티 클래스로 인스턴스 생성 방지
    }
    
    /**
     * API 관련 상수
     */
    public static final class Api {
        public static final int REQUEST_TIMEOUT_MS = 10000;
        public static final int CONNECT_TIMEOUT_MS = 5000;
        public static final int READ_TIMEOUT_MS = 10000;
        public static final int MAX_RETRY_COUNT = 3;
        public static final int RETRY_BASE_DELAY_MS = 1000;
        public static final int API_RATE_LIMIT_DELAY_MS = 100;
        
        // 한국 수출입은행 API
        public static final String KOREA_EXIM_BASE_URL = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON";
        public static final String KOREA_EXIM_DATA_CODE = "AP01";
        public static final int KOREA_EXIM_DAILY_LIMIT = 1000;
        
        private Api() {}
    }
    
    /**
     * 캐시 관련 상수
     */
    public static final class Cache {
        // TTL 시간 (분)
        public static final int DEFAULT_TTL_MINUTES = 5;
        public static final int EXCHANGE_RATE_TTL_MINUTES = 5;
        public static final int REALTIME_RATE_TTL_MINUTES = 5;
        public static final int HISTORICAL_RATE_TTL_MINUTES = 10;
        public static final int NEWS_TTL_MINUTES = 30;
        public static final int BANK_INFO_TTL_MINUTES = 30;
        public static final int EXCHANGE_CALCULATION_TTL_MINUTES = 2;
        
        // FCM 관련 캐시
        public static final int FCM_DUPLICATE_TTL_HOURS = 24;
        public static final int FCM_FAILED_TOKENS_TTL_HOURS = 6;
        
        // 분산 락
        public static final int DISTRIBUTED_LOCK_TTL_MINUTES = 10;
        
        // 캐시 이름
        public static final String EXCHANGE_RATES = "exchangeRates";
        public static final String REALTIME_RATE = "realtimeRate";
        public static final String HISTORICAL_RATE = "historicalRate";
        public static final String EXCHANGE_NEWS = "exchangeNews";
        public static final String CURRENCY_NEWS = "currencyNews";
        public static final String NEWS = "news";
        public static final String EXCHANGE_CALCULATION = "exchangeCalculation";
        public static final String BANK_EXCHANGE_INFO = "bankExchangeInfo";
        public static final String FCM_DUPLICATE = "fcmDuplicate";
        public static final String FCM_FAILED_TOKENS = "fcmFailedTokens";
        public static final String DISTRIBUTED_LOCK = "distributedLock";
        
        private Cache() {}
    }
    
    /**
     * FCM 관련 상수
     */
    public static final class Fcm {
        public static final int BATCH_SIZE = 500;
        public static final int MAX_RETRY_COUNT = 2;
        public static final int RETRY_BASE_DELAY_MS = 1000;
        
        // FCM 알림 타입
        public static final String TARGET_RATE_ACHIEVED = "TARGET_RATE_ACHIEVED";
        public static final String DAILY_RATE_ALERT = "DAILY_RATE_ALERT";
        
        private Fcm() {}
    }
    
    /**
     * 페이지네이션 관련 상수
     */
    public static final class Pagination {
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;
        public static final int MIN_PAGE_SIZE = 1;
        public static final int MIN_PAGE_NUMBER = 0;
        
        private Pagination() {}
    }
    
    /**
     * 환율 관련 상수
     */
    public static final class Exchange {
        public static final int CHART_DEFAULT_DAYS = 30;
        public static final int HISTORICAL_MAX_DAYS = 365;
        public static final int DECIMAL_SCALE = 4;
        
        // 100 단위 통화 코드 (수출입은행 API에서 100 단위로 제공되는 통화)
        public static final String JPY_100_UNIT = "JPY(100)";  // 일본 엔 (100엔 단위)
        public static final String IDR_100_UNIT = "IDR(100)";  // 인도네시아 루피아 (100 단위)
        
        // 통화 코드 매핑 (100단위 아님, 단순 코드 변환)
        public static final String CNH_CODE = "CNH"; // 중국 위안 (수출입은행에서는 CNY가 아닌 CNH 사용)
        
        private Exchange() {}
    }
    
    /**
     * 이미지 관련 상수
     */
    public static final class Image {
        public static final String DEFAULT_FLAG_IMAGE = "/images/flags/default.svg";
        public static final String FLAG_IMAGE_PATH = "/images/flags/";
        public static final String FLAG_IMAGE_EXTENSION = ".svg";
        
        private Image() {}
    }
    
    /**
     * 뉴스 관련 상수
     */
    public static final class News {
        public static final int DEFAULT_NEWS_SIZE = 10;
        public static final int MAX_NEWS_SIZE = 50;
        public static final String DEFAULT_SORT_ORDER = "publishedDate";
        
        private News() {}
    }
}