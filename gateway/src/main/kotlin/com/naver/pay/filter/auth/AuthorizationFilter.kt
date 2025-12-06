package com.naver.pay.filter.auth

import com.naver.pay.filter.util.getClientInfo
import com.naver.pay.filter.GatewayFilterOrder
import com.naver.pay.response.ErrorResponseUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * URL 접근 권한 확인 필터
 * 
 * 역할:
 * - PathAuthorizationPolicy를 사용하여 경로별 접근 권한 확인
 * - 권한 부족 시 403 Forbidden 응답
 */
@Component
class AuthorizationFilter: GlobalFilter, Ordered {
    
    private val logger = KotlinLogging.logger(AuthorizationFilter::class.java.name)
    private val pathAuthorizationPolicy = PathAuthorizationPolicy()

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.path.value()
        
        // 클라이언트 정보 조회
        val clientInfo = exchange.getClientInfo()
        
        // 클라이언트 정보가 없으면 인증 필터를 거치지 않은 것으로 간주
        if (clientInfo == null) {
            logger.warn { "클라이언트 정보가 없습니다. 경로: $path" }
            return ErrorResponseUtil.createUnauthorizedResponse(
                exchange,
                "Authentication required",
                path
            )
        }
        
        // 경로별 접근 권한 확인
        if (!pathAuthorizationPolicy.canAccess(clientInfo, path)) {
            logger.warn { 
                "클라이언트 ${clientInfo.clientId}의 경로 접근이 거부되었습니다. 경로: $path, 역할: ${clientInfo.role}" 
            }
            val errorMessage = pathAuthorizationPolicy.getAccessDeniedMessage(clientInfo, path)
            return ErrorResponseUtil.createForbiddenResponse(
                exchange,
                errorMessage,
                path
            )
        }
        
        return chain.filter(exchange)
    }
    
    override fun getOrder(): Int = GatewayFilterOrder.AUTHORIZATION
}

