package com.swyp.api_server.domain.rate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 외부에서 enum 타입으로 안전하게 접근 가능한 싱글톤 DTO
 * 사용법: ExchangeList.INSTANCE 또는 ExchangeList.instance()
 */
public enum ExchangeList {
    /** 싱글톤 인스턴스 */
    INSTANCE;

    /**
     * 필요에 따라 값을 추가하세요 (예: 환율 코드 또는 거래소 식별자)
     */
    public enum ExchangeType {
        USD("USD", "미국달러"),
        JPY("JPY", "엔화"),
        EUR("EUR", "유로화"),
        CNY("CNY", "위안화"),
        HKD("HKD", "홍콩달러"),
        TWD("TWD", "대만달러"),
        THB("THB", "태국바트"),
        SGD("SGD", "싱가폴달러"),
        PHP("PHP", "필리핀페소"),
        IDR("IDR", "인도네시아루피아"),
        MYR("MYR", "말레이시아링킷"),
        GBP("GBP", "영국파운드");
//        달러(캐나다)
//        CAD
//        달러(호주)
//        AUD
//        달러(뉴질랜드)
//        NZD
//        프랑(스위스)
//        CHF


        private final String code;
        private final String label;

        ExchangeType(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        /**
         * 모든 ExchangeType을 code와 label 모두 포함해 문자열 리스트로 반환
         * 예: ["USD", "달러(미국)", "JPY", "엔(일본)", ...]
         */
        public static List<String> all() {
            java.util.ArrayList<String> list = new java.util.ArrayList<>();
            for (ExchangeType t : values()) {
//                list.add(t.code);
                list.add(t.label);
            }
            return java.util.Collections.unmodifiableList(list);
        }
    }

    // 싱글톤이므로 공유 컬렉션은 스레드 안전하게 유지
    private final CopyOnWriteArrayList<ExchangeType> list = new CopyOnWriteArrayList<>();

    /**
     * 읽기 전용 뷰 제공
     */
    public List<ExchangeType> getList() {
        return Collections.unmodifiableList(list);
    }

    /**
     * Enum으로 추가
     */
    public void add(ExchangeType type) {
        if (type != null) {
            list.add(type);
        }
    }

    /**
     * 문자열로 들어온 값을 Enum으로 변환하여 추가 (대소문자 무시)
     */
    public void add(String type) {
        if (type == null || type.isBlank()) return;
        list.add(ExchangeType.valueOf(type.trim().toUpperCase()));
    }

    /**
     * 가독성을 위한 헬퍼 (INSTANCE 반환)
     */
    public static ExchangeList instance() {
        return INSTANCE;
    }
}
