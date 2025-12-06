package com.naver.pay.filter.auth

import com.naver.pay.filter.GatewayFilterOrder
import com.naver.pay.filter.auth.exception.ClientNotFoundException
import com.naver.pay.filter.util.HmacProperties
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.data.redis.core.ReactiveValueOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

class ApiKeyAuthenticationFilterTest : BehaviorSpec({
    val hmacProperties = HmacProperties(secret = "test-secret-key")
    val redisTemplate = mockk<ReactiveRedisTemplate<String, Any>>()
    val valueOperations = mockk<ReactiveValueOperations<String, Any>>()
    val clientLookupService = mockk<ClientLookupService>()
    
    val apiKeyAuthenticationFilter = ApiKeyAuthenticationFilter(
        redisTemplate = redisTemplate,
        hmacProperties = hmacProperties,
        clientLookupService = clientLookupService
    )

    afterTest {
        clearAllMocks()
    }
    
    Given("Actuator 경로에 접근할 때") {
        val request = MockServerHttpRequest.get("/actuator/health").build()
        val exchange = MockServerWebExchange.from(request)
        val chain = mockk<GatewayFilterChain> {
            every { filter(any()) } returns Mono.empty()
        }
        
        When("필터를 실행하면") {
            val result = apiKeyAuthenticationFilter.filter(exchange, chain)
            
            Then("인증 없이 다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
                exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] shouldBe null
            }
        }
    }
    
    Given("API Key가 없는 요청이 주어졌을 때") {
        val anonymousClientId = "test-anonymous-client-id"
        
        When("Authorization 헤더가 없으면") {
            val request = MockServerHttpRequest.get("/api/v1/urls")
                .header(ClientIdFilter.CLIENT_ID_HEADER, anonymousClientId)
                .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<GatewayFilterChain> {
                every { filter(any()) } returns Mono.empty()
            }
            
            val result = apiKeyAuthenticationFilter.filter(exchange, chain)
            
            Then("ANONYMOUS 클라이언트로 처리하고 다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
                val clientInfo = exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] as? ClientInfo
                clientInfo shouldBe ClientInfo(
                    clientId = anonymousClientId,
                    role = ClientRole.ANONYMOUS
                )
            }
        }
        
        When("Authorization 헤더가 비어있으면") {
            val request = MockServerHttpRequest.get("/api/v1/urls")
                .header(ClientIdFilter.CLIENT_ID_HEADER, anonymousClientId)
                .header(HttpHeaders.AUTHORIZATION, "")
                .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<GatewayFilterChain> {
                every { filter(any()) } returns Mono.empty()
            }
            
            val result = apiKeyAuthenticationFilter.filter(exchange, chain)
            
            Then("ANONYMOUS 클라이언트로 처리하고 다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
                val clientInfo = exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] as? ClientInfo
                clientInfo shouldBe ClientInfo(
                    clientId = anonymousClientId,
                    role = ClientRole.ANONYMOUS
                )
            }
        }
    }
    
    Given("API Key가 있는 요청이 주어졌을 때") {
        val apiKey = "test-api-key"
        val clientId = "test-client"
        val clientInfo = ClientInfo(
            clientId = clientId,
            role = ClientRole.USER
        )
        
        When("Bearer 토큰 형식으로 API Key가 있고 Redis 캐시에 클라이언트 정보가 있으면") {
            val request = MockServerHttpRequest.get("/api/v1/urls")
                .header(ClientIdFilter.CLIENT_ID_HEADER, clientId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<GatewayFilterChain> {
                every { filter(any()) } returns Mono.empty()
            }

            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(any<String>()) } returns Mono.just(clientInfo)
            every { clientLookupService.lookupClientInfo(any(), clientId) } returns Mono.just(clientInfo)


            val result = apiKeyAuthenticationFilter.filter(exchange, chain)
            
            Then("캐시에서 조회하고 다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
                val savedClientInfo = exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] as? ClientInfo
                savedClientInfo shouldBe clientInfo
            }
        }
        
        When("API Key만 있고 Redis 캐시에 클라이언트 정보가 있으면") {
            val request = MockServerHttpRequest.get("/api/v1/urls")
                .header(ClientIdFilter.CLIENT_ID_HEADER, clientId)
                .header(HttpHeaders.AUTHORIZATION, apiKey)
                .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<GatewayFilterChain> {
                every { filter(any()) } returns Mono.empty()
            }
            
            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(any<String>()) } returns Mono.just(clientInfo)
            every { clientLookupService.lookupClientInfo(any(), clientId) } returns Mono.just(clientInfo)

            val result = apiKeyAuthenticationFilter.filter(exchange, chain)
            
            Then("캐시에서 조회하고 다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
                val savedClientInfo = exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] as? ClientInfo
                savedClientInfo shouldBe clientInfo
            }
        }
        
        When("Redis 캐시에 클라이언트 정보가 없고 Lookup Service에서 조회 성공하면") {
            val request = MockServerHttpRequest.get("/api/v1/urls")
                .header(ClientIdFilter.CLIENT_ID_HEADER, clientId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<GatewayFilterChain> {
                every { filter(any()) } returns Mono.empty()
            }
            
            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(any<String>()) } returns Mono.empty()
            every { valueOperations.set(any<String>(), any<ClientInfo>(), any<Duration>()) } returns Mono.just(true)
            every { clientLookupService.lookupClientInfo(any(), clientId) } returns Mono.just(clientInfo)
            
            val result = apiKeyAuthenticationFilter.filter(exchange, chain)
            
            Then("Lookup Service에서 조회하고 캐시에 저장한 후 다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
                verify(exactly = 1) { clientLookupService.lookupClientInfo(any(), clientId) }
                verify(exactly = 1) { valueOperations.set(any<String>(), any<ClientInfo>(), any<Duration>()) }
                val savedClientInfo = exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] as? ClientInfo
                savedClientInfo shouldBe clientInfo
            }
        }
        
        When("Lookup Service에서 클라이언트를 찾을 수 없으면") {
            val request = MockServerHttpRequest.get("/api/v1/urls")
                .header(ClientIdFilter.CLIENT_ID_HEADER, clientId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<GatewayFilterChain>()
            
            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(any<String>()) } returns Mono.empty()
            every { clientLookupService.lookupClientInfo(any(), clientId) } returns Mono.error(
                ClientNotFoundException("클라이언트를 찾을 수 없습니다.")
            )
            
            val result = apiKeyAuthenticationFilter.filter(exchange, chain)
            
            Then("401 Unauthorized 응답을 반환한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                exchange.response.statusCode shouldBe HttpStatus.UNAUTHORIZED
                exchange.response.headers.getFirst("Content-Type") shouldBe "application/json"
                verify(exactly = 0) { chain.filter(any()) }
            }
        }
        
        When("예상치 못한 에러가 발생하면") {
            val request = MockServerHttpRequest.get("/api/v1/urls")
                .header(ClientIdFilter.CLIENT_ID_HEADER, clientId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<GatewayFilterChain>()
            
            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.get(any<String>()) } returns Mono.error(RuntimeException("Unexpected error"))
            every { clientLookupService.lookupClientInfo(any(), clientId) } returns Mono.just(clientInfo)

            val result = apiKeyAuthenticationFilter.filter(exchange, chain)
            
            Then("401 Unauthorized 응답을 반환한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                exchange.response.statusCode shouldBe HttpStatus.UNAUTHORIZED
                exchange.response.headers.getFirst("Content-Type") shouldBe "application/json"
                verify(exactly = 0) { chain.filter(any()) }
            }
        }
    }
    
    Given("필터 순서가 주어졌을 때") {
        When("getOrder()를 호출하면") {
            val order = apiKeyAuthenticationFilter.getOrder()
            
            Then("AUTHENTICATION 순서를 반환한다") {
                order shouldBe GatewayFilterOrder.AUTHENTICATION
            }
        }
    }
})

