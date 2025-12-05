## 이벤트 스키마 및 상세 설계

본 플랫폼은 Apache Kafka를 사용하여 이벤트 기반 아키텍처를 구현합니다.

### 토픽 설정

| 항목                 | 값 |
|--------------------|----|
| 파티션 수              | 3 |
| Replication Factor | 3 |
| Consumer Group     | `short-url.stats-processor` |
| Concurrency        | 3 |
 | MIN_ISR            | 2 |

### 이벤트 발행 전략

#### Outbox 패턴 (`short_url.created`)

- 트랜잭션 내에서 `event_publication` 테이블에 이벤트 저장
- `outbox-polling-publisher`가 0.5초 주기로 폴링
- Kafka로 이벤트 발행 후 `published_at` 업데이트
- At-least-once 보장 (멱등 컨슈머 필요)

#### 비동기 발행 (`short_url.clicked`)

- 리다이렉트 응답 후 비동기로 이벤트 발행
- 응답 시간에 영향을 주지 않도록 `sync: false` 설정
- Fire-and-forget 방식

---

### short_url.created
Short URL이 생성되었을 때 발행되는 이벤트입니다.

- **발행 주체**: `url-service` (Outbox 패턴)
- **소비 주체**: `short-url-stats-consumer`
- **파티션 키**: `shortKey`
- **Offset 전략**: `earliest` (모든 이벤트 처리)
- **직렬화**: `JSON`
 
**페이로드 스키마**

| 필드 | 타입 | 설명 |
|------|------|------|
| `shortKey` | String | 생성된 Short URL의 고유 키 |
| `shortUrl` | String | 완전한 Short URL (예: `https://short.naver.com/abc123`) |
| `originalUrl` | String | 원본 URL |
| `shortUrlCreatedAt` | Instant (ISO 8601) | Short URL 생성 시각 |
| `shortUrlExpiredAt` | Instant (ISO 8601) | Short URL 만료 시각 |

**처리 로직**:
- 통계 메타데이터 초기화 (MongoDB)

**예시**:

```json
{
  "shortKey": "abc123",
  "shortUrl": "https://short.naver.com/abc123",
  "originalUrl": "https://naver.com/long/url",
  "shortUrlCreatedAt": "2025-01-15T10:30:00Z",
  "shortUrlExpiredAt": "2025-02-14T10:30:00Z"
}
```

이 이벤트는 다음과 같은 특성으로 인해 **Outbox 패턴**을 통해 발행됩니다:

1. **이벤트의 중요성과 빈도**: 
   - Short URL 생성은 일일 약 1,000,000건으로 클릭 이벤트에 비해 낮은 빈도이지만, 각 이벤트는 통계 시스템의 메타데이터 초기화에 필수적입니다.
   - 하나의 이벤트 손실도 해당 Short URL의 통계 데이터 누락을 의미하므로, 이벤트 손실이 허용되지 않습니다.
   - 추후의 확장 시, 생성 이벤트를 구독하는 소비자들은 엄밀한 발행 보장을 요구할 확률이 높을 것으로 추론됩니다.   

2. **통계 메타데이터 초기화의 필수성**:
   - `short-url-stats-consumer`는 이 이벤트를 소비하여 MongoDB에 통계 메타데이터(`ShortUrlTotalStats`)를 초기화합니다.
   - 메타데이터에는 `shortUrl`, `originalUrl`, `shortUrlCreatedAt`, `shortUrlExpiredAt` 등이 포함되며, 이후 클릭 이벤트 처리의 기반이 됩니다.
   - 생성 이벤트가 누락되면 해당 Short URL의 통계 조회 시 데이터가 존재하지 않아 오류가 발생할 수 있습니다.

3. **트랜잭션 일관성 요구사항**:
   - Short URL 생성은 MySQL 트랜잭션 내에서 수행되며, 통계 메타데이터 초기화도 동일한 시점에 처리되어야 합니다.
   - DB 커밋과 이벤트 발행이 분리되면, DB에는 저장되었지만 통계 시스템에는 반영되지 않는 불일치 상태가 발생할 수 있습니다.
   - Outbox 패턴을 통해 트랜잭션 커밋과 이벤트 발행의 원자성을 보장합니다.

4. **Offset 전략의 의미**:
   - `earliest` 전략을 사용하여 Consumer가 시작될 때 과거의 모든 생성 이벤트를 처리합니다.
   - 이는 시스템이 재시작되거나 새로 배포되어도 모든 Short URL의 메타데이터를 복구할 수 있음을 의미합니다.

5. **이벤트 손실의 영향**:
   - 생성 이벤트가 손실되면 해당 Short URL의 통계 조회 API(`GET /api/v1/urls/{shortKey}`)에서 메타데이터가 누락됩니다.
   - 클릭 이벤트는 처리되지만 메타데이터가 없어 통계 집계에 문제가 발생할 수 있습니다.
   - 따라서 이벤트 손실을 방지하기 위해 Outbox 패턴을 통해 안전하게 저장하고 재발행할 수 있도록 합니다.

---

### short_url.clicked

Short URL이 클릭되어 리다이렉트될 때 발행되는 이벤트입니다.

- **발행 주체**: `redirect-service` (비동기 발행)
- **소비 주체**: `short-url-stats-consumer`
- **파티션 키**: `shortKey`
- **Offset 전략**: `latest` (실시간 이벤트만 처리)
- **직렬화**: `JSON`

**페이로드 스키마**:

| 필드 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `shortKey` | String | - | 클릭된 Short URL의 고유 키 |
| `referrer` | String | `"Direct"` | 리퍼러 URL (없을 경우 "Direct") |
| `userAgent` | String | `"Unknown"` | 사용자 에이전트 문자열 |
| `clickedAt` | Instant (ISO 8601) | - | 클릭 시각 |

**처리 로직**:
- MongoDB에 원자적 업데이트 (일별/디바이스별/Referrer별 클릭 수 증가)
- Redis Sorted Set에 실시간 집계 데이터 저장
- User-Agent 파싱을 통한 디바이스 타입 추출 (Mobile/Desktop/Tablet 등)

**예시**:

```json
{
  "shortKey": "abc123",
  "referrer": "https://naver.com",
  "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15",
  "clickedAt": "2025-01-15T10:30:00Z"
}
```

이 이벤트는 **비동기 발행(Fire-and-forget)** 방식을 사용합니다. 이는 다음과 같은 이유 때문입니다:

1. **응답 시간 최우선**: 리다이렉트 요청은 매우 높은 빈도(일일 약 10,000,000건)로 발생하며, 10ms 이내의 응답 시간이 요구됩니다. 이벤트 발행이 동기적으로 처리되면 응답 시간에 직접적인 영향을 미치므로, 비동기 발행을 통해 응답 시간을 보장합니다.

2. **이벤트 성질**: 클릭 이벤트는 통계 집계를 위한 부가 정보이며, 리다이렉트 성공 여부와 직접적인 연관이 없습니다. 즉, 이벤트 발행 실패가 사용자 경험에 영향을 주지 않아야 합니다.

3. **소비자 기대 동작**:
   - `short-url-stats-consumer`는 실시간 통계 집계를 위해 이 이벤트를 소비합니다.
   - Offset 전략을 `latest`로 설정하여 실시간 이벤트만 처리하며, 과거 이벤트 손실은 허용 가능합니다.
   - 대량의 클릭 이벤트를 처리해야 하므로, 이벤트 발행 실패 시 재시도보다는 실시간 처리에 집중합니다.

4. **부하 분산**: 리다이렉트 서비스의 부하를 최소화하기 위해 `sync: false` 설정으로 비동기 발행을 보장합니다. Kafka Producer의 내부 버퍼링을 활용하여 네트워크 지연의 영향을 최소화합니다.

5. **트레이드오프**: 
   - 일부 이벤트 손실 가능성을 감수하더라도, 리다이렉트 응답 시간을 보장하는 것이 더 중요합니다.
   - 통계 데이터는 실시간성이 중요하지만, 일부 누락은 전체 통계의 정확도에 큰 영향을 주지 않습니다.



## 참고

- 모든 이벤트는 JSON 형식으로 직렬화됩니다.
- 파티션 키는 `shortKey`로 설정되어 동일한 Short URL의 이벤트는 동일한 파티션으로 라우팅됩니다.