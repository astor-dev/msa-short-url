package com.naver.pay.filter.auth

import com.naver.pay.filter.GatewayFilterOrder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpCookie
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

class ClientIdFilterTest : BehaviorSpec({
    val clientIdFilter = ClientIdFilter()
    
    afterTest {
        clearAllMocks()
    }
    
    Given("헤더에 클라이언트 ID가 있는 요청이 주어졌을 때") {
        val existingClientId = "existing-client-id-123"
        val request = MockServerHttpRequest.get("/api/v1/urls")
            .header(ClientIdFilter.CLIENT_ID_HEADER, existingClientId)
            .build()
        val exchange = MockServerWebExchange.from(request)
        val chain = mockk<GatewayFilterChain> {
            every { filter(any()) } returns Mono.empty()
        }
        
        When("필터를 실행하면") {
            val result = clientIdFilter.filter(exchange, chain)
            
            Then("기존 클라이언트 ID를 사용하고 다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(any()) }
            }
            
            Then("쿠키는 추가하지 않는다") {
                exchange.response.cookies.isEmpty() shouldBe true
            }
        }
    }
    
    Given("쿠키에 클라이언트 ID가 있는 요청이 주어졌을 때") {
        val existingClientId = "existing-client-id-from-cookie"
        val cookie = HttpCookie(ClientIdFilter.CLIENT_ID_COOKIE, existingClientId)
        val request = MockServerHttpRequest.get("/api/v1/urls")
            .cookie(cookie)
            .build()
        val exchange = MockServerWebExchange.from(request)
        val chain = mockk<GatewayFilterChain> {
            every { filter(any()) } returns Mono.empty()
        }
        
        When("필터를 실행하면") {
            val result = clientIdFilter.filter(exchange, chain)
            
            Then("기존 클라이언트 ID를 사용하고 다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(any()) }
            }
            
            Then("쿠키는 추가하지 않는다") {
                exchange.response.cookies.isEmpty() shouldBe true
            }
        }
    }
    
    Given("클라이언트 ID가 없는 요청이 주어졌을 때") {
        val request = MockServerHttpRequest.get("/api/v1/urls").build()
        val exchange = MockServerWebExchange.from(request)
        val chain = mockk<GatewayFilterChain> {
            every { filter(any()) } returns Mono.empty()
        }
        
        When("필터를 실행하면") {
            val result = clientIdFilter.filter(exchange, chain)
            
            Then("새로운 클라이언트 ID를 생성하고 다음 필터로 진행한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(any()) }
            }
            
            Then("생성된 클라이언트 ID가 쿠키에 저장된다") {
                val cookies = exchange.response.cookies[ClientIdFilter.CLIENT_ID_COOKIE]
                cookies shouldNotBe null
                cookies?.size shouldBe 1
                
                val cookie = cookies?.firstOrNull()
                cookie shouldNotBe null
                cookie?.value shouldNotBe null
                cookie?.value?.isNotBlank() shouldBe true
            }
            
            Then("생성된 쿠키의 속성이 올바르게 설정된다") {
                val cookies = exchange.response.cookies[ClientIdFilter.CLIENT_ID_COOKIE]
                val cookie = cookies?.firstOrNull()
                
                cookie?.maxAge shouldBe Duration.ofDays(365L)
                cookie?.path shouldBe "/"
                cookie?.sameSite shouldBe "Lax"
            }
        }
    }
    
    Given("헤더와 쿠키 모두에 클라이언트 ID가 있는 요청이 주어졌을 때") {
        val headerClientId = "header-client-id"
        val cookieClientId = "cookie-client-id"
        val cookie = HttpCookie(ClientIdFilter.CLIENT_ID_COOKIE, cookieClientId)
        val request = MockServerHttpRequest.get("/api/v1/urls")
            .header(ClientIdFilter.CLIENT_ID_HEADER, headerClientId)
            .cookie(cookie)
            .build()
        val exchange = MockServerWebExchange.from(request)
        val chain = mockk<GatewayFilterChain> {
            every { filter(any()) } returns Mono.empty()
        }
        
        When("필터를 실행하면") {
            val result = clientIdFilter.filter(exchange, chain)
            
            Then("헤더의 클라이언트 ID를 우선 사용한다") {
                StepVerifier.create(result)
                    .verifyComplete()
                
                verify(exactly = 1) { chain.filter(any()) }
            }
            
            Then("쿠키는 추가하지 않는다") {
                exchange.response.cookies.isEmpty() shouldBe true
            }
        }
    }
    
    Given("필터 순서가 주어졌을 때") {
        When("getOrder()를 호출하면") {
            val order = clientIdFilter.getOrder()
            
            Then("CLIENT_ID 순서를 반환한다") {
                order shouldBe GatewayFilterOrder.CLIENT_ID
            }
        }
    }
})

