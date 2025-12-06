package com.naver.pay.filter.auth

import com.naver.pay.filter.GatewayFilterOrder
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

/**
 * 클라이언트 ID 생성 및 부착 필터
 * 
 * 역할:
 * - 요청 쿠키 또는 헤더에서 클라이언트 ID 확인
 * - 클라이언트 ID가 없으면 난수(UUID)로 생성
 * - 생성한 클라이언트 ID를 쿠키에 저장하여 다음 요청에 자동 포함
 * - 생성한 클라이언트 ID를 X-Client-Id 헤더에 부착
 * - 익명 클라이언트(브라우저, 프론트엔드)를 위한 고유 식별자 제공
 */
@Component
class ClientIdFilter : GlobalFilter, Ordered {
    
    private val logger = KotlinLogging.logger(ClientIdFilter::class.java.name)
    
    companion object {
        const val CLIENT_ID_HEADER = "X-Client-Id"
        const val CLIENT_ID_COOKIE = "CLIENT_ID"
        private const val COOKIE_MAX_AGE_DAYS = 365L
    }
    
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        
        // 쿠키 또는 헤더에서 클라이언트 ID 확인
        val existingClientId = getClientIdFromRequest(request)
        
        if (!existingClientId.isNullOrBlank()) {
            logger.debug { "기존 클라이언트 ID 사용: $existingClientId" }
            val mutatedRequest = request.mutate()
                .header(CLIENT_ID_HEADER, existingClientId)
                .build()
            
            val mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build()
            
            return chain.filter(mutatedExchange)
        }
        
        // 클라이언트 ID가 없으면 난수로 생성
        val clientId = UUID.randomUUID().toString()
        logger.debug { "새로운 클라이언트 ID 생성: $clientId" }
        
        // 헤더에 클라이언트 ID 추가
        val mutatedRequest = request.mutate()
            .header(CLIENT_ID_HEADER, clientId)
            .build()
        
        // 쿠키에 클라이언트 ID 저장 (다음 요청에 자동 포함)
        val responseCookie = ResponseCookie.from(CLIENT_ID_COOKIE, clientId)
            .maxAge(Duration.ofDays(COOKIE_MAX_AGE_DAYS))
            .path("/")
            .httpOnly(false) // JavaScript에서 접근 가능하도록 설정
            .sameSite("Lax")
            .build()
        
        val mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build()
        
        mutatedExchange.response.addCookie(responseCookie)
        
        return chain.filter(mutatedExchange)
    }
    
    private fun getClientIdFromRequest(request: ServerHttpRequest): String? {
        // 1. 헤더에서 확인
        val headerClientId = request.headers.getFirst(CLIENT_ID_HEADER)
        if (!headerClientId.isNullOrBlank()) {
            return headerClientId
        }
        
        // 2. 쿠키에서 확인
        val cookies = request.cookies[CLIENT_ID_COOKIE]
        return cookies?.firstOrNull()?.value
    }
    
    override fun getOrder(): Int = GatewayFilterOrder.CLIENT_ID
}
