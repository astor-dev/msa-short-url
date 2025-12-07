# API 명세

## 인증

| 접속 유형 | 권한 | 사용 가능한 API | 인증 방법 |
|---------|------|---------------|---------|
| 일반 접속 | 없음 | Short URL 조회(리다이렉트) | 인증 불필요 |
| 유저 접속 | User | Short URL 생성, Short URL 상태 조회 | `Authorization: Bearer test-user-key` |
| 어드민 접속 | Admin | 통계 조회 (상세 통계, Top N 집계) | `Authorization: Bearer test-admin-key` |

---

## API 목록

| 번호 | API 명 | 메서드 | 엔드포인트 | 인증 | 설명 |
|-----|------|--------|----------|------|------|
| 1 | Short URL 생성 | `POST` | `/api/v1/urls` | User | Short URL 생성 |
| 2 | Short URL 조회(리다이렉트) | `GET` | `/{shortKey}` | 없음 | Short URL 리다이렉트 |
| 3 | Short URL 상태 조회 | `GET` | `/api/v1/urls/{shortKey}` | User | Short URL 기본 정보 및 클릭 요약 조회 |
| 4 | Short URL 상세 통계 조회 | `GET` | `/api/v1/urls/{shortKey}/statistics` | Admin | Short URL 상세 통계 정보 조회 |
| 5 | Short URL 집계 Top N (일간) | `GET` | `/api/v1/statistics/top` | Admin | 플랫폼 전체 일간 Top N 통계 조회 |

---

## API 상세 명세

### 1. Short URL 생성 API

| 항목 | 내용 |
|-----|------|
| **메서드** | `POST` |
| **엔드포인트** | `/api/v1/urls` |
| **인증** | `Authorization: Bearer test-user-key` |
| **Content-Type** | `application/json` |

**Request Body**

```json
{
  "originalUrl": "https://naver.com/long/url",
  "ttlSeconds": 2592000
}
```

**Response (201 Created)**

```json
{
  "shortKey": "abc123",
  "shortUrl": "https://short.example.com/abc123",
  "originalUrl": "https://naver.com/long/url",
  "createdAt": "2025-01-15T10:30:00Z",
  "expiredAt": "2025-02-14T10:30:00Z"
}
```

**특징**
- 즉시 응답
- 동일 key에 대한 멱등성 보장

---

### 2. Short URL 조회(리다이렉트)

| 항목 | 내용 |
|-----|------|
| **메서드** | `GET` |
| **엔드포인트** | `/{shortKey}` |
| **인증** | 불필요 |
| **헤더** | `User-Agent`, `Referer` (선택) |

**Request Example**

```http
GET /abc123
User-Agent: Mozilla/5.0
Referer: https://pay.naver.com/events/card-benefit
```

**Response (302 Found)**

```http
302 Found
Location: https://naver.com/long/url
```

**특징**
- 1일 + jitter(0~60분) 동안 캐싱
- 클릭 **비동기 이벤트** 발행

---

### 3. Short URL 상태 조회 API

| 항목 | 내용 |
|-----|------|
| **메서드** | `GET` |
| **엔드포인트** | `/api/v1/urls/{shortKey}` |
| **인증** | `Authorization: Bearer test-user-key` |

**Request Example**

```http
GET /api/v1/urls/abc123
Authorization: Bearer test-user-key
```

**Response**

```json
{
  "shortKey": "abc123",
  "shortUrl": "https://short.example.com/abc123",
  "originalUrl": "...",
  "createdAt": "2025-01-15T10:30:00Z",
  "expiredAt": "2025-02-14T10:30:00Z",
  "clickSummary": {
    "totalClicks": 12345,
    "lastClickedAt": "2025-01-20T09:15:00Z"
  }
}
```

**응답 항목**
- 원본 URL, 단축 키/단축 URL
- 생성 시각, 만료 시각
- 전체 클릭수 및 마지막 클릭 시각(요약 정보 수준)

---

### 4. Short URL 상세 통계 조회 API

| 항목 | 내용 |
|-----|------|
| **메서드** | `GET` |
| **엔드포인트** | `/api/v1/urls/{shortKey}/statistics` |
| **인증** | `Authorization: Bearer test-admin-key` |

**Request Example**

```http
GET /api/v1/urls/abc123/statistics
Authorization: Bearer test-admin-key
```

**Response**

```json
{
  "shortKey": "abc123",
  "totalClicks": 12345,
  "byDate": [
    { "date": "2025-01-18", "clicks": 1200 },
    { "date": "2025-01-19", "clicks": 3400 }
  ],
  "byDevice": [
    { "deviceType": "mobile", "clicks": 8000 },
    { "deviceType": "desktop", "clicks": 4000 }
  ],
  "byReferrer": [
    { "referrer": "https://google.com", "clicks": 5000 },
    { "referrer": "https://naver.com", "clicks": 3000 }
  ]
}
```

**응답 항목**
- 전체 클릭수(`totalClicks`)
- 일자별 클릭수(`byDate`)
- 디바이스 타입별 클릭수(`byDevice`)
- referrer URL 별 클릭수(`byReferrer`)

---

### 5. Short URL 집계 Top N API (일간)

| 항목 | 내용 |
|-----|------|
| **메서드** | `GET` |
| **엔드포인트** | `/api/v1/statistics/top` |
| **인증** | `Authorization: Bearer test-admin-key` |
| **Query Parameters** | `date` (필수), `limit` (선택, 기본값: 10, 최대: 100) |

**Request Example**

```http
GET /api/v1/statistics/top?date=2025-01-20&limit=10
Authorization: Bearer test-admin-key
```

**Response**

```json
{  
  "date": "2025-01-20",  
  "topUrls": [  
    {  
      "rank": 1,  
      "shortKey": "abc123",  
      "shortUrl": "https://short.example.com/abc123",  
      "originalUrl": "https://example.com/landing",  
      "totalClicks": 15000
    },  
    {  
      "rank": 2,  
      "shortKey": "xyz987",  
      "shortUrl": "https://short.example.com/xyz987",  
      "originalUrl": "https://example.com/event",  
      "totalClicks": 12000  
    }  
  ],  
  "topReferrers": [  
    {  
      "rank": 1,  
      "referrer": "https://naver.com",  
      "totalClicks": 22000
    },  
    {  
      "rank": 2,  
      "referrer": "https://pay.naver.com",  
      "totalClicks": 15000  
    }  
  ],  
  "topByDevice": [  
    {  
      "deviceType": "mobile",  
      "totalClicks": 30000,
      "topUrls": [  
        {  
          "rank": 1,  
          "shortKey": "m001",  
          "shortUrl": "https://short.example.com/m001",  
          "originalUrl": "https://example.com/mobile-landing",  
          "clicksFromThisDevice": 7000  
        }  
      ]  
    },  
    {  
      "deviceType": "desktop",  
      "totalClicks": 18000,  
      "topUrls": [  
        {  
          "rank": 1,  
          "shortKey": "d001",  
          "shortUrl": "https://short.example.com/d001",  
          "originalUrl": "https://example.com/desktop-landing",  
          "clicksFromThisDevice": 6000  
        }  
      ]  
    }  
  ]  
}
```

**설명**
- **플랫폼 전체 기준, "일단위" 집계 Top N 통계**를 조회

**집계 데이터**

| 집계 유형 | 응답 필드 | 항목 |
|---------|---------|------|
| 가장 많이 클릭된 Short URL Top N | `topUrls[]` | `shortKey`, `shortUrl`, `originalUrl`, `totalClicks`, `rank` |
| referrer 기준 클릭 Top N | `topReferrers[]` | `referrer`, `totalClicks`, `rank` |
| 디바이스별 클릭 Top N | `topByDevice[]` | `deviceType`(mobile/desktop/tablet 등), `totalClicks`, `topUrls[]`<br/>- topUrls: `shortKey`, `shortUrl`, `originalUrl`, `clicksFromThisDevice`, `rank` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 최대값 | 설명 |
|---------|------|------|--------|--------|------|
| `date` | String | ✅ | - | - | 조회할 날짜 (예: 2025-01-20) |
| `limit` | Integer | ❌ | 10 | 100 | 각 Top 리스트에서 가져올 개수 |
