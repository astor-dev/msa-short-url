# Traffic Generator

Short URL Platform의 부하 테스트 및 성능 검증을 위한 트래픽 생성기입니다.

## 📋 목차

- [기능](#-기능)
- [빌드 및 실행](#-빌드-및-실행)
- [시나리오](#-시나리오)
- [설정 옵션](#-설정-옵션)
- [리포트](#-리포트)
- [주의사항](#-주의사항)

## 🚀 기능

- **URL 생성 부하 테스트**: URL 생성 API의 부하 테스트 및 멱등성 검증
- **리다이렉트 부하 테스트**: 리다이렉트 API의 캐싱 전략 및 대량 트래픽 처리 검증
- **상태 조회 부하 테스트**: 상태 조회 API의 성능 검증
- **통계 조회 부하 테스트**: 상세 통계 및 Top N 통계 조회 성능 검증
- **실시간 메트릭 수집**: 요청 성공률, 응답 시간, 에러 분석 등 실시간 수집
- **JSON 리포트 생성**: 테스트 결과를 JSON 형식으로 자동 저장

## 🔨 빌드 및 실행

### 빌드

```bash
./gradlew :traffic-generator:build
```

### 실행

```bash
./gradlew :traffic-generator:run --console=plain
```

실행 후 대화형 메뉴를 통해 시나리오를 선택하고 설정을 변경할 수 있습니다.

## 📊 시나리오

### 1. URL 생성 (CREATE)

- **설명**: Short URL 생성 API에 대한 부하 테스트
- **용도**: 
  - 멱등성 검증
  - 동시성 제어 검증
  - 생성 API 성능 측정

### 2. 리다이렉트 (REDIRECT)

- **설명**: Short URL 리다이렉트 API에 대한 부하 테스트
- **용도**:
  - 캐싱 전략 검증
  - 대량 트래픽 처리 성능 측정
  - 캐시 히트율 분석
- **사용 키**: 미리 정의된 테스트 키 사용 (`MQ`, `Mg`, `Mw`, `NA`, `NQ`, `Ng`)

### 3. 상태 조회 (STATE)

- **설명**: Short URL 상태 조회 API에 대한 부하 테스트
- **용도**:
  - 상태 조회 API 성능 측정
  - 캐시 활용도 분석
- **사용 키**: 미리 정의된 테스트 키 사용 (`MQ`, `Mg`, `Mw`, `NA`, `NQ`, `Ng`)

### 4. 통계 (STATISTICS)

- **설명**: 상세 통계 조회 API에 대한 부하 테스트
- **용도**:
  - 통계 집계 성능 측정
  - Redis/MongoDB 조회 성능 분석
- **사용 키**: 미리 정의된 테스트 키 사용 (`MQ`, `Mg`, `Mw`, `NA`, `NQ`, `Ng`)

### 5. 상위 N개 (TOP_N)

- **설명**: Top N 통계 조회 API에 대한 부하 테스트
- **용도**:
  - Top N 조회 성능 측정
  - Sorted Set 활용 성능 분석

## ⚙️ 설정 옵션

### 기본 설정

| 설정 항목 | 기본값 | 설명 |
|---------|-------|------|
| `baseUrl` | `http://localhost:8080` | API Gateway 기본 URL |
| `threads` | `10` | 동시 실행 스레드 수 |
| `count` | `100` | 총 요청 수 (요청 수 기준 모드) |
| `durationSeconds` | `null` | 실행 시간(초) (시간 기준 모드) |
| `requestIntervalMs` | `0` | 요청 간격(밀리초) |
| `timeoutSeconds` | `30` | 요청 타임아웃(초) |
| `userAuthToken` | `test-user-key` | 사용자 인증 토큰 |
| `adminAuthToken` | `test-admin-key` | 관리자 인증 토큰 |
| `userAgent` | `TrafficGenerator/1.0` | User-Agent 헤더 |
| `referer` | `null` | Referer 헤더 (선택사항) |

### 실행 모드

#### 1. 요청 수 기준 모드

- 총 요청 수를 지정하여 테스트 실행
- 예: 1000개의 요청을 생성

#### 2. 시간 기준 모드

- 실행 시간을 지정하여 해당 시간 동안 지속적으로 요청 생성
- 예: 60초 동안 지속적으로 요청 생성

### 설정 변경

실행 중 메인 메뉴에서 "설정 변경"을 선택하여 각 설정 항목을 수정할 수 있습니다.

## 📈 리포트

### 리포트 저장 위치

리포트는 `traffic-generator/reports/` 디렉토리에 자동으로 저장됩니다.

### 파일 명명 규칙

```
{시나리오명}_{타임스탬프}.json
```

예: `CREATE_20251209_120000.json`, `REDIRECT_20251209_120030.json`

### 리포트 형식

```json
{
  "scenario": "CREATE",
  "duration": 5,
  "config": {
    "baseUrl": "http://localhost:8080",
    "threads": 10,
    "count": 100,
    "durationSeconds": null,
    "requestIntervalMs": 0,
    "timeoutSeconds": 30,
    "userAuthToken": "test-user-key",
    "adminAuthToken": "test-admin-key",
    "userAgent": "TrafficGenerator/1.0",
    "referer": null
  },
  "totalRequests": 100,
  "successfulRequests": 100,
  "failedRequests": 0,
  "responseTime": {
    "average": 51,
    "p50": 31,
    "p95": 253,
    "p99": 253,
    "min": 15,
    "max": 254
  },
  "errorBreakdown": {},
  "firstErrorElapsedTime": null
}
```

### 리포트 항목 설명

| 항목 | 설명 |
|-----|------|
| `scenario` | 실행한 시나리오 이름 |
| `duration` | 전체 실행 시간(초) |
| `config` | 실행 시 사용된 설정값 |
| `totalRequests` | 총 요청 수 |
| `successfulRequests` | 성공한 요청 수 |
| `failedRequests` | 실패한 요청 수 |
| `responseTime` | 응답 시간 통계 (평균, P50, P95, P99, 최소, 최대) |
| `errorBreakdown` | HTTP 상태 코드별 에러 발생 횟수 |
| `firstErrorElapsedTime` | 최초 에러 발생 시점까지의 경과 시간(밀리초), 에러가 없으면 `null` |

## ⚠️ 주의사항

### 조회 API 호출 전 필수 사항

**조회 API(상태 조회, 통계 조회, 상위 N개 조회)를 호출하기 전에 반드시 URL 생성 API를 최소 10회 이상 호출해야 합니다.**

이유:
- 조회 API는 미리 생성된 Short URL을 대상으로 동작합니다
- 테스트 키(`MQ`, `Mg`, `Mw`, `NA`, `NQ`, `Ng`)는 미리 생성되어 있어야 하며, 실제 데이터가 존재해야 정확한 성능 측정이 가능합니다
- 생성 API를 먼저 호출하여 데이터를 준비한 후 조회 API 테스트를 진행하는 것을 권장합니다

### 권장 테스트 순서

1. **URL 생성 시나리오 실행** (최소 10회 이상)
2. **리다이렉트 시나리오 실행**
3. **상태 조회 시나리오 실행**
4. **통계 조회 시나리오 실행**
5. **상위 N개 조회 시나리오 실행**

### 기타 주의사항

- **동시 실행 스레드 수**: 시스템 리소스를 고려하여 적절한 스레드 수를 설정하세요
- **요청 간격**: `requestIntervalMs`를 0으로 설정하면 최대 부하를 생성합니다. 시스템 안정성을 위해 적절한 간격을 설정하는 것을 권장합니다
- **타임아웃**: 네트워크 지연이나 서버 부하 상황을 고려하여 충분한 타임아웃 값을 설정하세요
- **인증 토큰**: 실제 환경에서는 유효한 인증 토큰을 사용해야 합니다
