# MSA 기반 Short URL 분산 시스템 플랫폼 
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
./gradlew :short-url-stats:batch:bootrun

# Outbox 도메인
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

#### short-url 도메인

Short URL 생성 및 리다이렉트를 담당하는 핵심 도메인입니다.

**실행 단위:**

| 모듈 | 포트 | 주요 기능                                 | 기술 스택                                            |
|------|------|---------------------------------------|--------------------------------------------------|
| **url-service** | 8081 | Short URL 생성, 중복 방지, 동시 진입 방지, 멱등성 보장 | Spring Boot Web, Redis, JPA, Spring Cloud Stream |
| **redirect-service** | 8082 | 리다이렉트 처리, 캐시 기반 조회                    | Spring Boot Web, JPA, Redis, Spring Cloud Stream |

**도메인:**

| 도메인           | 기능                                                        |
|---------------|-----------------------------------------------------------|
| **short-url** | Short URL 코어 도메인 - ID 기반 Base64 인코딩으로 Short Key 생성, RDBMS 저장, 캐싱, 이벤트 발행     |

#### short-url-stats 도메인

Short URL 통계 집계 및 조회를 담당하는 도메인입니다.

**실행 단위:**

| 모듈                        | 포트 | 주요 기능 | 기술 스택                                            |
|---------------------------|------|----------|--------------------------------------------------|
| **stats-service**         | 8083 | 통계 조회 API (상태/상세/Top N)               | Spring Boot Web, MongoDB, Redis                  |
| **consumer**              | 8091 | Kafka 이벤트 소비, 통계 집계 (MongoDB/Redis) | Spring Boot, Spring Cloud Stream, MongoDB, Redis |
| **batch**                 | 8092 | 일별 Top N 통계 배치 집계 및 영속화 | Spring Boot, Spring Batch, JPA, MongoDB, Redis   |

**도메인:**

| 도메인                 | 기능                                                                      |
|---------------------|-------------------------------------------------------------------------|
| **total-stats**     | Short URL 누적 통계 집계 및 조회 - MongoDB 상세 저장, 원자적 연산                         |
| **daily-top-stats** | Short URL 일간 통계 집계 및 조회 - Redis Sorted Set 실시간 집계, 원자적 연산, MongoDB에 영속화 |

#### outbox 도메인

이벤트 발행의 트랜잭션 일관성을 보장하는 도메인입니다.

**실행 단위:**

| 모듈         | 포트 | 주요 기능 | 기술 스택                                            |
|------------|------|----------|--------------------------------------------------|
| **worker** | 8090 | Outbox 패턴 구현, 이벤트 발행 (0.5초 주기, 배치 50건) | Spring Boot, Spring Cloud Stream, JPA            |

**도메인:**

| 도메인 | 기능                                                        |
|------|-----------------------------------------------------------|
| **outbox** | Outbox 패턴 구현에 필요한 기능 모듈 - 트랜잭션과 이벤트 발행 일관성 보장, `FOR UPDATE SKIP LOCKED` 분산 환경 지원    |

### 인프라 모듈

도메인과 무관한 공통 인프라 및 유틸리티를 제공합니다.

#### gateway

| 모듈 | 포트 | 주요 기능                                 | 기술 스택                                            |
|------|------|---------------------------------------|--------------------------------------------------|
| **gateway** | 8080 | API Gateway, 라우팅 처리                   | Spring Cloud Gateway (WebFlux)                   |

#### util 모듈

| 모듈 | 기능                                                              |
|------|-----------------------------------------------------------------|
| **distributed-lock** | 분산 환경에서 락을 통한 동시성 제어를 제공 - Redisson 기반 분산 락 실행기, 락 획득/해제 자동화, 예외 상황에서도 락 해제 보장             |
| **object-mapper** | 공통 Jackson ObjectMapper 유틸리티 모듈 - JavaTimeModule, KotlinModule 등 공통 모듈 등록, 프로젝트 전역에서 일관된 직렬화/역직렬화 |


## 주요 파이프라인

### Short URL 생성

![short-url](docs/images/create-short-url.png)

Short URL 생성은 **멱등성 보장**과 **동시성 제어**를 핵심으로 설계되었습니다.

#### Key 생성 알고리즘

DB가 할당한 ID(auto_increment)를 **Base64 URL-Safe (No Padding)** 형식으로 인코딩합니다.

**트레이드오프**:
- **장점**: 
  - 해시 기반 방식과 비교하여 길이가 짧고 충돌 가능성이 없습니다.
  - 인코딩/디코딩이 단순하여 성능 오버헤드가 적습니다.
- **단점**: 
  - 클라이언트가 ShortKey를 복호화하여 ID를 알 수 있습니다.
  - 이를 통해PK가 누설되어 다음 URL을 예측할 수 있는 등 보안상 취약할 수 있습니다.

Short URL 서비스의 특성상 **짧은 길이**와 **충돌 방지**가 더 중요하다 판단했습니다.

#### 동시성 제어

**분산 락 (Redis)**: 
- 원본 URL을 락 키로 사용하여 동일한 URL에 대한 동시 생성 요청을 방지합니다.
- originalUrl 기반 유효성 검사 이후 쓰기 작업이 진행 중 일 때 다른 트랜잭션이 커밋되어 중복 생성 및 멱등성이 깨지는 것을 막기 위함입니다.
- Critical section 내에서 캐시 확인, DB 조회, 생성 로직이 원자적으로 실행됩니다.

#### 이벤트 발행

**Outbox 패턴**:
- 트랜잭션 내에서 `event_publication` 테이블에 이벤트를 저장합니다.
- `outbox-worker`가 0.5초 주기로 폴링하여 Kafka로 발행합니다.
- 트랜잭션 커밋과 이벤트 발행의 원자성을 보장하여 이벤트 손실을 방지합니다.

**Stats Consumer**:
- 통계 도메인에서 활용할 Document를 생성 이벤트 Payload 기반 비동기로 생성합니다.
- `INSERT IF NOT EXISTS`를 통해 생성의 멱등성을 보장합니다.
