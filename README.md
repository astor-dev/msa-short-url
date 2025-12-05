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
### 개별 서비스 로컬 실행 (예: 게이트웨이)
```shell
./gradlew :api:gateway:bootrun  
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
api/
  ├─ gateway
  ├─ url-service
  ├─ redirect-service
  ├─ stats-service
  └─ build.gradle.kts

worker/
  ├─ outbox-polling-publisher
  ├─ short-url-stats-batch
  ├─ short-url-stats-consumer
  └─ build.gradle.kts

domain/
  ├─ shorturl
  ├─ resolved-short-url
  ├─ short-url-stats
  ├─ outbox
  └─ build.gradle.kts

util/
  ├─ distributed-lock
  ├─ object-mapper
  └─ build.gradle.kts

build.gradle.kts
```

### 모듈 분할의 기준

본 플랫폼은 **기능별 책임 분리**와 **트래픽 특성**에 따라 모듈을 분리했습니다.

- 별도의 **런타임**을 가질 수 있으며 특정 트래픽을 응집해 처리하는 모듈을 **실행 모듈**로 분류합니다. 
- 특정 기능/도메인 단위로 응집되며 타 모듈에서 가져와 사용할 수 있는 모듈을 **기능 모듈**로 분류합니다.



### 실행 모듈

별도의 런타임을 가질 수 있으며 독립 배포 및 스케일링이 가능한 모듈입니다. `:bootrun`을 통해 실행 가능한 스프링부트 애플리케이션입니다.

#### api 모듈

| 모듈 | 포트 | 주요 기능                                 | 기술 스택                                            |
|------|------|---------------------------------------|--------------------------------------------------|
| **gateway** | 8080 | API Gateway, 라우팅 처리                   | Spring Cloud Gateway (WebFlux)                   |
| **url-service** | 8081 | Short URL 생성, 중복 방지, 동시 진입 방지, 멱등성 보장 | Spring Boot Web, Redis, JPA, Spring Cloud Stream |
| **redirect-service** | 8082 | 리다이렉트 처리, 캐시 기반 조회                    | Spring Boot Web, JPA, Redis, Spring Cloud Stream |
| **stats-service** | 8083 | 통계 조회 API (상태/상세/Top N)               | Spring Boot Web, MongoDB, Redis                  |

#### worker 모듈

| 모듈 | 포트 | 주요 기능 | 기술 스택                                            |
|------|------|----------|--------------------------------------------------|
| **outbox-polling-publisher** | 8090 | Outbox 패턴 구현, 이벤트 발행 (0.5초 주기, 배치 50건) | Spring Boot, Spring Cloud Stream, JPA            |
| **short-url-stats-consumer** | 8091 | Kafka 이벤트 소비, 통계 집계 (MongoDB/Redis) | Spring Boot, Spring Cloud Stream, MongoDB, Redis |
| **short-url-stats-batch** | 8092 | 일별 Top N 통계 배치 집계 및 영속화 | Spring Boot, Spring Batch, JPA, MongoDB, Redis   |

### 기능 모듈

특정 기능/도메인 단위로 응집되며 타 모듈에서 가져와 사용할 수 있는 모듈입니다. 독립적인 런타임을 가지지 않으며, 실행 모듈에 의존성으로 포함되어 사용됩니다.

#### domain 모듈

| 모듈 | 정의                        | 기능                                                        |
|------|---------------------------|-----------------------------------------------------------|
| **short-url** | Short URL 코어 도메인          | ID 기반 Base64 인코딩으로 Short Key 생성, RDBMS 저장, 캐싱, 이벤트 발행     |
| **resolved-short-url** | 메타 정보 등 정보가 결합된 Short URL | Short URL과 메타 정보 결합, 캐시 기반 빠른 연산, 영속성 계층과의 통합             |
| **short-url-stats** | Short URL 통계 집계 및 조회      | MongoDB 상세 저장, Redis Sorted Set 실시간 집계, Lua Script 원자적 연산 |
| **outbox** | Outbox 패턴 구현에 필요한 기능 모듈   | 트랜잭션과 이벤트 발행 일관성 보장, `FOR UPDATE SKIP LOCKED` 분산 환경 지원    |

#### util 모듈

| 모듈 | 정의                           | 기능                                                              |
|------|------------------------------|-----------------------------------------------------------------|
| **distributed-lock** | 분산 환경에서 락을 통한 동시성 제어를 제공     | Redisson 기반 분산 락 실행기, 락 획득/해제 자동화, 예외 상황에서도 락 해제 보장             |
| **object-mapper** | 공통 Jackson ObjectMapper 유틸리티 모듈 | JavaTimeModule, KotlinModule 등 공통 모듈 등록, 프로젝트 전역에서 일관된 직렬화/역직렬화 |

