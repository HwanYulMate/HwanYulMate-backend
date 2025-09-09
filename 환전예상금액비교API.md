# 환전 예상 금액 비교 API

**설명:** 실시간 환율 정보를 기반으로 우대율과 수수료를 적용하여 환전 예상 금액을 계산하고 7개 은행별 비교가 가능합니다.

- **토큰 필요 여부:** ❌

---

## 1. 전체 은행 환전 계산 (POST)

**Method & Path:**

```
POST /api/exchange/calculate
```

### 요청 본문 (Request Body)

```json
{
  "currencyCode": "USD",
  "amount": 1000.00,
  "direction": "FOREIGN_TO_KRW",
  "specificBank": "KB국민은행"
}
```

### 요청 필드

| 필드 | 설명 |
| --- | --- |
| currencyCode | 환전할 통화 코드 (필수, 14개국 지원) |
| amount | 환전할 금액 (필수, 0.01 이상) |
| direction | 환전 방향 (`FOREIGN_TO_KRW` 또는 `KRW_TO_FOREIGN`) |
| specificBank | 특정 은행만 조회 (선택사항) |

### 성공 응답 예시

```json
[
  {
    "bankName": "IM뱅크",
    "bankCode": "031",
    "baseRate": 1385.20,
    "appliedRate": 1372.78,
    "preferentialRate": 85.0,
    "spreadRate": 0.9,
    "totalFee": 500.0,
    "feeDetail": {
      "fixedFee": 500.0,
      "feeRate": 0.03,
      "rateBasedFee": 41.32
    },
    "finalAmount": 1372280.0,
    "inputAmount": 1000.0,
    "currencyCode": "USD",
    "flagImageUrl": "/images/flags/us.svg",
    "isOnlineAvailable": true,
    "description": "디지털뱅크 최고 우대율 85%"
  },
  {
    "bankName": "우리은행",
    "bankCode": "020",
    "baseRate": 1385.20,
    "appliedRate": 1371.37,
    "preferentialRate": 80.0,
    "spreadRate": 1.0,
    "totalFee": 0.0,
    "feeDetail": {
      "fixedFee": 0.0,
      "feeRate": 0.0,
      "rateBasedFee": 0.0
    },
    "finalAmount": 1371370.0,
    "inputAmount": 1000.0,
    "currencyCode": "USD",
    "flagImageUrl": "/images/flags/us.svg",
    "isOnlineAvailable": true,
    "description": "위비뱅킹 80% 우대율, 수수료 무료"
  }
]
```

### 응답 필드

| 필드 | 설명 |
| --- | --- |
| bankName | 은행명 |
| bankCode | 은행 코드 |
| baseRate | 기준 환율 |
| appliedRate | 실제 적용 환율 (우대율 반영) |
| preferentialRate | 우대율 (%) |
| spreadRate | 스프레드율 |
| totalFee | 총 수수료 |
| feeDetail | 수수료 상세 정보 |
| finalAmount | 최종 환전 결과 금액 |
| inputAmount | 입력 금액 |
| currencyCode | 통화 코드 |
| flagImageUrl | 국기 이미지 URL |
| isOnlineAvailable | 온라인 환전 가능 여부 |
| description | 추가 설명 |

---

## 2. 간편 환전 계산 (GET)

**Method & Path:**

```
GET /api/exchange/calculate
```

### Query Parameters

| 필드 | 설명 |
| --- | --- |
| currencyCode | 통화 코드 (필수, 14개국 지원) |
| amount | 환전 금액 (필수) |
| direction | 환전 방향 (기본값: FOREIGN_TO_KRW) |
| bank | 특정 은행 지정 (선택사항) |

### 요청 예시

```
GET /api/exchange/calculate?currencyCode=USD&amount=1000&direction=FOREIGN_TO_KRW
GET /api/exchange/calculate?currencyCode=USD&amount=1000&bank=KB국민은행
```

**응답:** POST 방식과 동일한 JSON 배열

---

## 3. 특정 은행 환전 계산

**Method & Path:**

```
POST /api/exchange/calculate/{bankName}
```

### URL Parameters

| 이름 | 설명 |
| --- | --- |
| bankName | 은행명 (예: KB국민은행, 우리은행) |

### 요청 본문 (Request Body)

```json
{
  "currencyCode": "USD",
  "amount": 1000.00,
  "direction": "FOREIGN_TO_KRW"
}
```

### 성공 응답 예시

```json
{
  "bankName": "KB국민은행",
  "bankCode": "004",
  "baseRate": 1385.20,
  "appliedRate": 1374.65,
  "preferentialRate": 50.0,
  "spreadRate": 1.5,
  "totalFee": 1000.0,
  "feeDetail": {
    "fixedFee": 1000.0,
    "feeRate": 0.1,
    "rateBasedFee": 138.0
  },
  "finalAmount": 1374650.0,
  "inputAmount": 1000.0,
  "currencyCode": "USD",
  "flagImageUrl": "/images/flags/us.svg",
  "isOnlineAvailable": true,
  "description": "KB스타뱅킹 50% 우대율 적용"
}
```

---

## 4. 환전 방향

- `FOREIGN_TO_KRW`: 외화 → 원화 (예: 1000달러 → ?원)
- `KRW_TO_FOREIGN`: 원화 → 외화 (예: 100만원 → ?달러)

---

## 5. 계산 로직

1. 수출입은행 실시간 환율 조회
2. DB에서 **7개 은행** 정보 가져오기
3. 각 은행별 계산:
    - **우대율 적용:** 기준환율 × (1 - 우대율%)
    - **수수료 계산:** 고정수수료 + (금액 × 수수료율)
    - **최종 금액:** 환전금액 - 총 수수료
4. **JPY, IDR은 100단위 환율** 자동 처리

---

## 6. 지원 은행 (7개)

KB국민은행, 신한은행, 하나은행, 우리은행, SC제일은행, 한국씨티은행, IM뱅크

## 7. 지원 통화 (14개국)

USD, JPY, EUR, CNY, HKD, THB, SGD, IDR, MYR, GBP, CAD, AUD, NZD, CHF

---

## 8. 공통 에러

| HTTP Status | 에러 코드 | 메시지 |
| --- | --- | --- |
| 400 | RATE_003 | 유효하지 않은 통화 코드입니다 |
| 400 | RATE_004 | 환전 금액이 유효하지 않습니다 |
| 400 | RATE_005 | 최소 환전 금액 미달입니다 |
| 400 | RATE_006 | 최대 환전 금액 초과입니다 |
| 400 | RATE_007 | 지원하지 않는 은행입니다 |