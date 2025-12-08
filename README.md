# MSA 기반 Short URL 분산 시스템 플랫폼
![platform-overall](docs/images/platform-overall.png)

네이버 파이낸셜 Externship - TECH(BE)

author: 김도훈 (astor-dev)

- [api 명세](docs/API_SPEC.md)
- [이벤트 스키마 및 상세 설계](docs/EVENT_SCHEMA.md)
## 🚀 Getting Started

```shell
docker compose up -d --build
```
총 16개의 컨테이너가 실행됩니다. 실행 컴퓨터의 CPU 및 메모리 부하에 주의가 필요합니다.
### 인프라 컨테이너 실행
```shell
docker compose -f docker-compose.infrastructure.yml up -d --build  
```
서비스는 인프라 컨테이너가 모두 실행 된 후 실행해야 합니다.

### 서비스 컨테이너 실행
```shell
docker compose -f docker-compose.services.yml up -d --build  
```
6개의 서비스가 실행됩니다. CPU 및 메모리 부하에 주의가 필요합니다.
### 개별 서비스 로컬 실행

각 도메인의 실행 단위를 개별적으로 실행할 수 있습니다.

```shell
# Gateway
./gradlew :gateway:bootrun

# Short URL 도메인
./gradlew :short-url:api:url-service:bootrun
./gradlew :short-url:api:redirect-service:bootrun

# Short URL Stats 도메인
./gradlew :short-url-stats:api:stats-service:bootrun
./gradlew :short-url-stats:consumer:bootrun
java -jar short-url-stats/batch/build/libs/batch.jar --spring.profiles.active=total
java -jar short-url-stats/batch/build/libs/batch.jar --spring.profiles.active=daily

# Outbox
./gradlew :outbox:worker:bootrun
```

## 💾 인프라 컴포넌트

본 플랫폼은 다음 인프라 컴포넌트로 구성됩니다:

- **MySQL 8.0**: OLTP 데이터 저장소
  - Short URL 원천 데이터 저장
  - Outbox 패턴을 위한 이벤트 발행 테이블

- **MongoDB 7.0**: 통계 데이터 저장소
  - Short URL 통계 데이터 저장
  - 일별/디바이스별/Referrer별 집계 데이터 관리

- **Redis 7.4**: 캐시 및 실시간 통계 집계, 분산 락
  - Short URL 조회 캐시
  - 실시간 통계 집계를 위한 Sorted Set 활용
  - 클릭 수 집계 및 Top N 조회

- **Apache Kafka 4.1**: 이벤트 스트리밍 플랫폼
  - 3개 Controller 노드 (KRaft 모드)
  - 3개 Broker 노드
  - Replication Factor: 3, ISR: 2, Partition: 3, 
  - Kafka UI

---

## 📦 프로젝트 모듈 구조

```plaintext
gateway/
  └─ build.gradle.kts

short-url/
  ├─ api
  │  ├─ url-service
  │  └─ redirect-service
  └─ build.gradle.kts
  
short-url-stats/
  ├─ api
  │  └─ stats-service
  ├─ consumer
  ├─ batch  
  └─ build.gradle.kts

outbox/
  ├─ worker
  └─ build.gradle.kts

util/
  ├─ distributed-lock
  ├─ object-mapper
  └─ build.gradle.kts

build.gradle.kts
```

### 아키텍처 설계 원칙

- **도메인 단위 응집**: 비즈니스 도메인별로 최상위 모듈을 구성하여 높은 응집도를 유지합니다.
- **실행 단위 분리**: 각 도메인 내부에 독립적으로 배포 및 스케일링 가능한 실행 단위를 포함합니다.
- **도메인 모듈 공유**: 도메인 로직은 실행 단위 간 공유 가능하도록 설계했습니다.

### 도메인 모듈

각 도메인은 독립적인 비즈니스 영역을 담당하며, 내부에 실행 단위와 도메인 로직 모듈을 포함합니다.

#### 1. short-url 도메인

Short URL 생성 및 리다이렉트를 담당하는 핵심 도메인입니다.

| 도메인           | 기능                                                                         |
|---------------|----------------------------------------------------------------------------|
| **short-url** | ID 기반 Base64 인코딩으로 Short Key 생성, RDBMS 저장, 캐싱, 이벤트 발행                      |


| 모듈 | 포트 | 주요 기능                                 | 기술 스택                                            |
|------|------|---------------------------------------|--------------------------------------------------|
| **url-service** | 8081 | Short URL 생성, 중복 방지, 동시 진입 방지, 멱등성 보장 | Spring Boot Web, Redis, JPA, Spring Cloud Stream |
| **redirect-service** | 8082 | 리다이렉트 처리, 캐시 기반 조회                    | Spring Boot Web, JPA, Redis, Spring Cloud Stream |

#### 2. short-url-stats 도메인

Short URL 통계 집계 및 조회를 담당하는 도메인입니다.

| 도메인                 | 기능                                                                      |
|---------------------|-------------------------------------------------------------------------|
| **total-stats**     | Short URL 누적 통계 집계 및 조회 - MongoDB 상세 저장, 원자적 연산                         |
| **daily-top-stats** | Short URL 일간 통계 집계 및 조회 - Redis Sorted Set 실시간 집계, 원자적 연산, MongoDB에 영속화 |

| 모듈                        | 포트 | 주요 기능 | 기술 스택                                          |
|---------------------------|------|----------|------------------------------------------------|
| **stats-service**         | 8083 | 통계 조회 API (상태/상세/Top N)               | Spring Boot Web, MongoDB, Redis                |
| **consumer**              | 8091 | Kafka 이벤트 소비, 통계 집계 (MongoDB/Redis) | Spring Boot, Spring Cloud Stream, MongoDB, Redis |
| **batch**                 | 8092 | 일별 Top N 통계 배치 집계 및 영속화 | Spring Boot, Spring Batch, MongoDB, Redis   |

### 인프라 모듈

도메인과 무관한 공통 인프라 및 유틸리티를 제공합니다.

#### 1. gateway

| 모듈 | 포트 | 주요 기능                                                    | 기술 스택                                 |
|------|------|----------------------------------------------------------|---------------------------------------|
| **gateway** | 8080 | API Gateway, 라우팅 처리, 인증/인가, RateLimiting, CircuitBreaker | Spring Cloud Gateway (WebFlux), Redis |

#### 2. outbox

이벤트 발행의 트랜잭션 일관성을 보장하는 인프라 모듈입니다.

| 모듈 | 기능                                                        |
|------|-----------------------------------------------------------|
| **outbox** | Outbox 패턴 구현에 필요한 기능 모듈 - 트랜잭션과 이벤트 발행 일관성 보장, `FOR UPDATE SKIP LOCKED` 분산 환경 지원    |

| 모듈 | 포트 | 주요 기능 | 기술 스택                                            |
|------|------|----------|--------------------------------------------------|
| **worker** | 8090 | Outbox 패턴 구현, 이벤트 발행 (0.5초 주기, 배치 50건) | Spring Boot, Spring Cloud Stream, JPA            |

#### 3. util 모듈

| 모듈 | 기능                                                              |
|------|-----------------------------------------------------------------|
| **distributed-lock** | 분산 환경에서 락을 통한 동시성 제어를 제공 - Redisson 기반 분산 락 실행기, 락 획득/해제 자동화, 예외 상황에서도 락 해제 보장             |
| **object-mapper** | 공통 Jackson ObjectMapper 유틸리티 모듈 - JavaTimeModule, KotlinModule 등 공통 모듈 등록, 프로젝트 전역에서 일관된 직렬화/역직렬화 |


## ⚙️️ 주요 파이프라인

### Short URL 생성

![create-short-url](docs/images/create-short-url.png)

Short URL 생성은 **멱등성 보장**과 **동시성 제어**를 핵심으로 설계되었습니다.

#### 1. Key 생성 알고리즘

DB가 할당한 ID(auto_increment)를 **Base64 URL-Safe (No Padding)** 형식으로 인코딩합니다.

**트레이드오프**:
- **장점**: 
  - UUID/해시 기반 방식과 비교하여 길이가 짧고 충돌 가능성이 없습니다.
  - 인코딩/디코딩이 단순하여 성능 오버헤드가 적습니다.
- **단점**: 
  - 클라이언트가 ShortKey를 복호화하여 ID를 알 수 있습니다.
  - 이를 통해 PK가 누설되어 다음 URL을 예측할 수 있는 등 보안 상 취약할 수 있습니다.

Short URL 서비스의 특성상 **짧은 길이**와 **충돌 방지**가 더 중요하다 판단했습니다.

#### 2. 동시성 제어

**분산 락 (Redis)**: 
- 원본 URL을 락 키로 사용하여 동일한 URL에 대한 동시 생성 요청을 방지합니다.
- originalUrl 기반 유효성 검사 이후 쓰기 작업이 진행 중 일 때 다른 트랜잭션이 커밋되어 중복 생성 및 멱등성이 깨지는 것을 막기 위함입니다.
- Critical section 내에서 캐시 확인, DB 조회, 생성 로직이 원자적으로 실행됩니다.

#### 3. 이벤트

**Outbox 패턴**:
- 트랜잭션 내에서 `event_publication` 테이블에 이벤트를 저장합니다.
- `outbox-worker`가 0.5초 주기로 폴링하여 Kafka로 발행합니다.
- 트랜잭션 커밋과 이벤트 발행의 원자성을 보장하여 이벤트 손실을 방지합니다.

**Stats Consumer**:
- 통계 도메인에서 활용할 Document를 생성 이벤트 Payload 기반 비동기로 생성합니다.
- `INSERT IF NOT EXISTS`를 통해 생성의 멱등성을 보장합니다.

### Short URL 리다이렉트

![redirect-short-url](docs/images/redirect-short-url.png)

Short URL 리다이렉트는 **응답 시간 최우선**과 **캐시 최적화**를 핵심으로 설계되었습니다.

#### 1. 캐시 처리 전략

**Thundering Herd 방지**:
- 캐시 미스 발생 시 분산 락을 획득하여 동시에 여러 요청이 MySQL에 접근하는 것을 방지합니다.
- 락 획득 후 재차 캐시를 확인하여 다른 프로세스가 캐시를 채운 경우 early return합니다.

**Cache Stampede 방지**:
- 캐시 TTL에 0~60분의 랜덤 값(Jitter)을 추가하여 만료 시각을 분산시킵니다.
- 동일한 시각에 많은 캐시 항목이 동시에 만료되는 것을 방지합니다.

#### 2. 이벤트

**이벤트 발행**:
- 리다이렉트 응답 후 비동기로 클릭 이벤트를 발행합니다.
- `sync: false` 설정으로 응답 시간에 영향을 주지 않도록 보장합니다.
- `ack: 0` 설정으로 Fire-and-forget 방식으로 즉각 발행합니다.
- Outbox 패턴 없이 직접 Kafka로 발행하여 응답 지연을 최소화합니다.

**트레이드오프**:
- **장점**: 
  - 응답 시간에 영향을 주지 않아 10ms 이내 응답을 보장합니다.
  - 높은 처리량(일일 약 10,000,000건)을 처리할 수 있습니다.
- **단점**: 
  - 일부 이벤트 손실 가능성이 있습니다.
  - 이벤트 발행 실패 시 재시도하지 않습니다.

다량 쏟아지는 클릭 이벤트 특성 상 어느정도의 유실을 감수하고 성능을 챙기는 것이 낫다고 판단했습니다. 

**Stats Consumer**:
- MongoDB 부하를 방지하기 위해 Redis를 우선 활용합니다.
- **누적 통계**
  - `INCR` / `HINCRBY`로 Redis에 누적 클릭 수를 저장합니다.
  - 누적 통계 미스 시 MongoDB에서 초기값을 조회하여 Redis에 세팅합니다.
  - shortKey를 dirtySet에 저장합니다. 추후 배치에서 동기화할 url을 식별하는데 사용합니다. 
- **일간 통계**
  - `ZINCRBY`로 Redis Sorted Set에 일별 클릭 수를 집계합니다.
  - Sorted Set을 활용하여 Top N 조회 성능을 최적화합니다.
- **배치 동기화**
  - Redis에 저장된 통계 데이터를 배치 작업을 통해 MongoDB에 동기화합니다. (Write Back)
  - 실시간 집계는 Redis에서 수행하고, 영속화는 MongoDB에 비동기로 저장합니다.

### 캐시 동기화 배치 작업

캐시 동기화는 Redis를 중심으로 수집된 통계를 지연 반영(Write-Back) 방식으로 MongoDB에 저장하는 구조입니다. 실시간 경로의 가벼움과 최종 일관성 확보를 동시에 달성하는 것이 목적입니다.

#### 1. 누적 집계 동기화
![batch-total](docs/images/batch-total.png)

- Redis dirtySet에 마킹된 shortKey만을 대상으로 합니다. 
- `SPOP`을 사용하여 batch-size 단위로 처리하므로 메모리 및 I/O 부하를 제어할 수 있습니다.
- Reader는 pop된 키들에 대해 Redis에서 누적 통계(`HGETALL` / `GET` 등)를 조회합니다.
- Writer는 MongoDB에 `bulkWrite`로 반영합니다.

#### 2. 일간 Top N 통계 집계 동기화
![batch-daily](docs/images/batch-daily.png)
Redis Sorted Set에 누적된 클릭 데이터를 기반으로 랭킹 문서를 생성하는 작업입니다. 실시간 경로에서 집계한 값을 그대로 활용하여 MongoDB에 일 단위 스냅샷을 저장합니다.

## 확장 방향

### DLQ 활용 실패 토픽 격리

#### As-Is
- Kafka Consumer에서 메시지 처리 실패 시 재시도나 격리 메커니즘이 없음
- 일시적인 오류나 잘못된 형식의 메시지로 인해 Consumer가 멈추거나 메시지가 손실될 수 있음
- 실패한 메시지에 대한 추적 및 분석 불가능
- 에러 발생 시 수동 개입 없이는 복구 어려움

#### To-Be
- **Dead Letter Queue 토픽(DLT) 생성**: 실패한 메시지를 격리할 전용 토픽 구성
- **에러 핸들링 전략**:
  - 최대 재시도 횟수 설정 (예: 3회)
  - 백오프 전략 설정(지수적 백오프 등)
  - 재시도 후에도 실패한 메시지는 DLT로 전송
  - 원본 메시지와 함께 실패 원인(에러 메시지, 스택 트레이스)을 메타데이터로 저장
- **모니터링 및 알림**:
  - DLT의 메시지 수 모니터링
  - 임계값 초과 시 알림 발송

**기대 효과**:
- Consumer의 안정성 향상 (일부 실패 메시지로 인한 전체 Consumer 중단 방지)
- 실패한 메시지의 추적 및 분석 가능
- 데이터 손실 방지 및 복구 가능성 확보

### 인증/유저 서비스 추가

#### As-Is
- Gateway의 [ClientLookupService](gateway/src/main/kotlin/com/naver/pay/filter/auth/ClientLookupService.kt)에서 하드코딩된 Mock 클라이언트 정보 사용
- API Key 생성/관리 기능 없음
- 클라이언트 정보 동적 변경 불가능
- 운영 환경에서 사용 불가능한 상태

#### To-Be
- **유저 서비스 도메인 추가**:
  - API Key 생성/관리 기능
  - 클라이언트 정보 조회 API
  - 역할 및 권한 관리
  - API Key 활성화/비활성화 기능

- **Gateway 연동**
  - `ClientLookupService`를 유저 서비스와 통신하도록 변경

**기대 효과**:
- 실제 운영 환경에서 사용 가능한 인증/인가 시스템 구축
- 클라이언트별 세밀한 접근 제어 및 모니터링 가능
- API Key 생명주기 관리 및 보안 정책 적용 가능