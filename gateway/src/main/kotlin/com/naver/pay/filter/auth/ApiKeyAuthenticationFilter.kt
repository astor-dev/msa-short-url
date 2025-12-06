package com.naver.pay.filter.auth

import com.naver.pay.filter.GatewayFilterOrder
import com.naver.pay.filter.auth.exception.ClientNotFoundException
import com.naver.pay.filter.util.HmacProperties
import com.naver.pay.filter.util.MaskUtil
import com.naver.pay.filter.util.hmacSha256
import com.naver.pay.response.ErrorResponseUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * API Key 기반 인증 필터
 * 
 * 역할:
 * - X-API-Key 헤더에서 API Key 추출
 * - API Key가 있으면 클라이언트 정보 조회 (Redis 캐시 → Lookup Service)
 * - API Key가 없으면 ANONYMOUS 클라이언트로 처리
 * - 클라이언트 정보를 exchange에 저장
 */
@Component
class ApiKeyAuthenticationFilter(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val hmacProperties: HmacProperties,
    private val clientLookupService: ClientLookupService,
) : GlobalFilter, Ordered {
    
    private val logger = KotlinLogging.logger(ApiKeyAuthenticationFilter::class.java.name)
    
    companion object {
        const val CLIENT_INFO_ATTRIBUTE = "CLIENT_INFO"
        private const val CACHE_TTL_HOURS = 24L
    }
    
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.path.value()
        
        // Actuator 엔드포인트는 인증 제외
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange)
        }
        
        // API Key 헤더 추출
        val apiKey = parseApiKey(request)
        
        // API Key가 없으면 ANONYMOUS 클라이언트로 처리
        if (apiKey.isNullOrBlank()) {
            val anonymousClient = ClientInfo.createAnonymous()
            exchange.attributes[CLIENT_INFO_ATTRIBUTE] = anonymousClient
            logger.debug { "익명 클라이언트로 접근: 경로: $path" }
            return chain.filter(exchange)
        }

        
        // 클라이언트 정보 조회
        return findClientInfo(apiKey)
            .flatMap { clientInfo ->
                // 클라이언트 정보를 exchange에 저장
                exchange.attributes[CLIENT_INFO_ATTRIBUTE] = clientInfo
                logger.debug { "인증 성공: 클라이언트 ${clientInfo.clientId}, 경로: $path" }
                chain.filter(exchange)
            }
            .onErrorResume { error ->
                when (error) {
                    is ClientNotFoundException -> {
                        logger.warn { "API 키에 해당하는 클라이언트를 찾을 수 없습니다. API 키: ${MaskUtil.maskApiKey(apiKey)}" }
                        ErrorResponseUtil.createUnauthorizedResponse(
                            exchange,
                            "Invalid API key",
                            path
                        )
                    }
                    else -> {
                        logger.error(error) { "인증 중 예상치 못한 오류가 발생했습니다." }
                        ErrorResponseUtil.createUnauthorizedResponse(
                            exchange,
                            "Authentication failed",
                            path
                        )
                    }
                }
            }
    }

    /**
     * API Key로 클라이언트 정보를 조회합니다.
     * 
     * 1. Redis 캐시에서 조회 시도
     * 2. 캐시 미스 시 Lookup Service에 요청
     * 3. 조회 성공 시 Redis에 캐싱
     */
    private fun findClientInfo(apiKey: String): Mono<ClientInfo> {
        val hashedKey = hmacSha256(apiKey, hmacProperties)
        val cacheKey = "${CacheNames.CLIENT_INFO}::$hashedKey"
        
        // 1. Redis 캐시에서 조회
        return redisTemplate.opsForValue()
            .get(cacheKey)
            .cast(ClientInfo::class.java)
            .doOnNext { 
                logger.debug { "캐시에서 클라이언트 정보 조회 성공: apiKey=${MaskUtil.maskApiKey(apiKey)}" }
            }
            .switchIfEmpty(
                // 2. 캐시 미스 시 Lookup Service에 요청
                clientLookupService.lookupClientInfo(hashedKey)
                    .flatMap { clientInfo ->
                        // 3. 조회 성공 시 Redis에 캐싱
                        logger.debug { "Lookup Service에서 조회한 클라이언트 정보를 캐시에 저장: clientId=${clientInfo.clientId}" }
                        redisTemplate.opsForValue()
                            .set(cacheKey, clientInfo, Duration.ofHours(CACHE_TTL_HOURS))
                            .thenReturn(clientInfo)
                    }
                    .doOnError { error ->
                        logger.warn(error) { "클라이언트 정보 조회 실패: apiKey=${MaskUtil.maskApiKey(apiKey)}" }
                    }
            )
    }

    private fun parseApiKey(request: ServerHttpRequest): String? {
        val apiKey = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        return apiKey?.split(" ")?.lastOrNull()
    }

    override fun getOrder(): Int = GatewayFilterOrder.AUTHENTICATION
}