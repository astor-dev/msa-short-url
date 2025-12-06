package com.naver.pay.auth

import org.springframework.web.server.ServerWebExchange

/**
 * ServerWebExchange에서 클라이언트 정보를 가져오는 확장 함수
 *
 * 인증 필터에서 설정한 클라이언트 정보를 조회할 수 있습니다.
 */
fun ServerWebExchange.getClientInfo(): ClientInfo? {
    return attributes[ApiKeyAuthenticationFilter.CLIENT_INFO_ATTRIBUTE] as? ClientInfo
}

