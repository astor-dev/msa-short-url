## API 명세

### Short URL 생성 API

* Request

```http
POST /api/v1/urls
Content-Type: application/json

{
  "originalUrl": "https://naver.com/long/url",
  "ttlSeconds": 2592000
}
```

* Response (201 Created)

```json
{
  "shortKey": "abc123",
  "shortUrl": "https://short.example.com/abc123",
  "originalUrl": "https://naver.com/long/url",
  "createdAt": "2025-01-15T10:30:00Z",
  "expiredAt": "2025-02-14T10:30:00Z"
}
```
- 즉시 응답
- 동일 key에 대한 멱등성 보장

### Short URL 조회(리다이렉트)

**Request**

```http
GET /abc123
User-Agent: Mozilla/5.0
Referer: https://pay.naver.com/events/card-benefit
```

**Response**

```http
302 Found
Location: https://naver.com/long/url
```
- 1일 + jitter(0~60분) 동안 캐싱
- 클릭 **비동기 이벤트** 발행

### 5.3 Short URL 상태 조회 API

**Request**

```http
GET /api/v1/urls/abc123
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

- 단축 URL에 대한 기본 메타 정보를 조회
- 항목 
  - 원본 URL, 단축 키/단축 URL 
  - 생성 시각, 만료 시각 
  - 전체 클릭수 및 마지막 클릭 시각(요약 정보 수준)

### 5.4 Short URL 상세 통계 조회 API

**Request**

```http
GET /api/v1/urls/abc123/statistics
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

- 단축 URL에 대한 **상세 통계 정보**를 조회
- 항목:
  - 전체 클릭수(`totalClicks`)
  - 일자별 클릭수(`byDate`)
  - 디바이스 타입별 클릭수(`byDevice`) 
  - referrer URL 별 클릭수(`byReferrer`)

### 5.5 Short URL 집계 Top N API (일간)

**Request**

```http
GET /api/v1/statistics/top?date=2025-01-20&limit=10
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
* **플랫폼 전체 기준, “일단위” 집계 Top N 통계**를 조회
* 집계 데이터

1. **가장 많이 클릭된 Short URL Top N**
    * 응답 필드: `topUrls[]`
    * 항목: `shortKey`, `shortUrl`, `originalUrl`, `totalClicks`, `rank`
2. **referrer 기준으로 가장 클릭이 많은 Top N**
    * 응답 필드: `topReferrers[]`
    * 항목: `referrer`, `totalClicks`, `rank`
3. **디바이스별로 가장 많이 클릭된 Short URL Top N**
    * 응답 필드: `topByDevice[]`
    * 항목:: `deviceType`(mobile/desktop/tablet 등), `totalClicks`, `topUrls[]`:
        * topUrls : `shortKey`, `shortUrl`, `originalUrl`, `clicksFromThisDevice`, `rank`

* `limit` 파라미터:
    * 각 Top 리스트에서 가져올 개수 (기본 10)
    * 상한 개수 100
