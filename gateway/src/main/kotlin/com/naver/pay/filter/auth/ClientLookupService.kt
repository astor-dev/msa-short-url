package com.naver.pay.filter.auth

import com.naver.pay.filter.auth.exception.ClientNotFoundException
import com.naver.pay.filter.util.HmacProperties
import com.naver.pay.filter.util.MaskUtil
import com.naver.pay.filter.util.hmacSha256
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * 클라이언트 정보를 조회하는 서비스
 * 
 * 역할:
 * - API Key로 클라이언트 정보 조회 (현재는 상수 기반 Mock 데이터 사용)
 * - 실제 마이크로서비스 연동 시 이 부분을 구현
 */
@Service
class ClientLookupService(
    private val hmacProperties: HmacProperties
) {
    
    private val logger = KotlinLogging.logger(ClientLookupService::class.java.name)

    private val mockClientMap = mapOf(
        "admin-api-key-12345" to ClientInfo(hmacSha256("admin-client-001", hmacProperties), ClientRole.ADMIN),
        "user-api-key-12345" to ClientInfo(hmacSha256("user-client-001", hmacProperties), ClientRole.USER),
        "test-admin-key" to ClientInfo(hmacSha256("test-admin-001", hmacProperties), ClientRole.ADMIN),
        "test-user-key" to ClientInfo(hmacSha256("test-user-001", hmacProperties), ClientRole.USER),
    )
    
    /**
     * API Key로 클라이언트 정보를 조회합니다.
     * 
     * TODO: 현재는 Mock 데이터를 사용하며, 실제 마이크로서비스 연동 시 이 부분을 구현합니다.
     * 
     * @param hashedApiKey HMAC256 해시된 API KEY
     * @return ClientInfo
     */
    fun lookupClientInfo(hashedApiKey: String): Mono<ClientInfo> {
        logger.debug { "클라이언트 정보 조회 시작: apiKey=${MaskUtil.maskApiKey(hashedApiKey)}" }
        
        return Mono.fromCallable {
            mockClientMap[hashedApiKey]
                ?: throw ClientNotFoundException("클라이언트를 찾을 수 없습니다. apiKey=${MaskUtil.maskApiKey(hashedApiKey)}")
        }
        .doOnSuccess { clientInfo ->
            logger.debug { "클라이언트 정보 조회 성공: clientId=${clientInfo.clientId}, role=${clientInfo.role}" }
        }
    }
}
