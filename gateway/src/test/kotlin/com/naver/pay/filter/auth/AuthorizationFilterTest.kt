package com.naver.pay.filter.auth

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class AuthorizationFilterTest : BehaviorSpec({
    val authorizationFilter = AuthorizationFilter()
    
    Given("클라이언트 정보가 없는 요청이 주어졌을 때") {
        val request = MockServerHttpRequest.get("/api/v1/urls").build()
        val exchange = MockServerWebExchange.from(request)
        val chain = mockk<GatewayFilterChain>()
        
        When("필터를 실행하면") {
            val result = authorizationFilter.filter(exchange, chain)
            
            Then("401 Unauthorized 응답을 반환한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                exchange.response.statusCode shouldBe HttpStatus.UNAUTHORIZED
                exchange.response.headers.getFirst("Content-Type") shouldBe "application/json"
            }
            
            Then("다음 필터로 진행하지 않는다") {
                verify(exactly = 0) { chain.filter(any()) }
            }
        }
    }
    
    Given("권한이 충분한 클라이언트가 주어졌을 때") {
        val userClientInfo = ClientInfo(
            clientId = "test-user",
            role = ClientRole.USER
        )
        val adminClientInfo = ClientInfo(
            clientId = "test-admin",
            role = ClientRole.ADMIN
        )
        
        When("USER 역할로 USER 권한이 필요한 경로에 접근하면") {
            val request = MockServerHttpRequest.get("/api/v1/urls").build()
            val exchange = MockServerWebExchange.from(request)
            exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] = userClientInfo
            val chain = mockk<GatewayFilterChain> {
                every { filter(any()) } returns Mono.empty()
            }
            
            val result = authorizationFilter.filter(exchange, chain)
            
            Then("다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
            }
        }
        
        When("ADMIN 역할로 ADMIN 권한이 필요한 통계 경로에 접근하면") {
            val request = MockServerHttpRequest.get("/api/v1/urls/testKey/statistics").build()
            val exchange = MockServerWebExchange.from(request)
            exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] = adminClientInfo
            val chain = mockk<GatewayFilterChain> {
                every { filter(any()) } returns Mono.empty()
            }
            
            val result = authorizationFilter.filter(exchange, chain)
            
            Then("다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
            }
        }
        
        When("ANONYMOUS 역할로 리다이렉트 경로에 접근하면") {
            val anonymousClientInfo = ClientInfo.createAnonymous()
            val request = MockServerHttpRequest.get("/testKey").build()
            val exchange = MockServerWebExchange.from(request)
            exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] = anonymousClientInfo
            val chain = mockk<GatewayFilterChain> {
                every { filter(any()) } returns Mono.empty()
            }
            
            val result = authorizationFilter.filter(exchange, chain)
            
            Then("다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
            }
        }
    }
    
    Given("권한이 부족한 클라이언트가 주어졌을 때") {
        When("ANONYMOUS 역할로 USER 권한이 필요한 경로에 접근하면") {
            val anonymousClientInfo = ClientInfo.createAnonymous()
            val request = MockServerHttpRequest.get("/api/v1/urls").build()
            val exchange = MockServerWebExchange.from(request)
            exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] = anonymousClientInfo
            val chain = mockk<GatewayFilterChain>()
            
            val result = authorizationFilter.filter(exchange, chain)
            
            Then("403 Forbidden 응답을 반환한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                exchange.response.statusCode shouldBe HttpStatus.FORBIDDEN
                exchange.response.headers.getFirst("Content-Type") shouldBe "application/json"
            }
            
            Then("다음 필터로 진행하지 않는다") {
                verify(exactly = 0) { chain.filter(any()) }
            }
        }
        
        When("USER 역할로 ADMIN 권한이 필요한 통계 경로에 접근하면") {
            val userClientInfo = ClientInfo(
                clientId = "test-user",
                role = ClientRole.USER
            )
            val request = MockServerHttpRequest.get("/api/v1/urls/testKey/statistics").build()
            val exchange = MockServerWebExchange.from(request)
            exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] = userClientInfo
            val chain = mockk<GatewayFilterChain>()
            
            val result = authorizationFilter.filter(exchange, chain)
            
            Then("403 Forbidden 응답을 반환한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                exchange.response.statusCode shouldBe HttpStatus.FORBIDDEN
                exchange.response.headers.getFirst("Content-Type") shouldBe "application/json"
            }
            
            Then("다음 필터로 진행하지 않는다") {
                verify(exactly = 0) { chain.filter(any()) }
            }
        }
        
        When("ANONYMOUS 역할로 ADMIN 권한이 필요한 통계 경로에 접근하면") {
            val anonymousClientInfo = ClientInfo.createAnonymous()
            val request = MockServerHttpRequest.get("/api/v1/statistics/test").build()
            val exchange = MockServerWebExchange.from(request)
            exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] = anonymousClientInfo
            val chain = mockk<GatewayFilterChain>()
            
            val result = authorizationFilter.filter(exchange, chain)
            
            Then("403 Forbidden 응답을 반환한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                exchange.response.statusCode shouldBe HttpStatus.FORBIDDEN
                exchange.response.headers.getFirst("Content-Type") shouldBe "application/json"
            }
            
            Then("다음 필터로 진행하지 않는다") {
                verify(exactly = 0) { chain.filter(any()) }
            }
        }
    }
    
    Given("Actuator 경로에 접근할 때") {
        val userClientInfo = ClientInfo(
            clientId = "test-user",
            role = ClientRole.USER
        )
        val request = MockServerHttpRequest.get("/actuator/health").build()
        val exchange = MockServerWebExchange.from(request)
        exchange.attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] = userClientInfo
        val chain = mockk<GatewayFilterChain> {
            every { filter(any()) } returns Mono.empty()
        }
        
        When("필터를 실행하면") {
            val result = authorizationFilter.filter(exchange, chain)
            
            Then("다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(exchange) }
            }
        }
    }
    
    Given("필터 순서가 주어졌을 때") {
        When("getOrder()를 호출하면") {
            val order = authorizationFilter.getOrder()
            
            Then("AUTHORIZATION 순서를 반환한다") {
                order shouldBe com.naver.pay.filter.GatewayFilterOrder.AUTHORIZATION
            }
        }
    }
})

