package com.naver.pay.filter.util

import com.naver.pay.filter.auth.ApiKeyAuthenticationFilter
import com.naver.pay.filter.auth.ClientInfo
import org.springframework.web.server.ServerWebExchange

/**
 * ServerWebExchange에서 클라이언트 정보를 가져오는 확장 함수
 *
 * 필터에서 설정한 클라이언트 정보를 조회할 수 있습니다.
 */
fun ServerWebExchange.getClientInfo(): ClientInfo? {
    return attributes[ApiKeyAuthenticationFilter.Companion.CLIENT_INFO_ATTRIBUTE] as? ClientInfo
}

