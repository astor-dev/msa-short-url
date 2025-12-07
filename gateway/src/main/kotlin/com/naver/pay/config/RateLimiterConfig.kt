package com.naver.pay.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono



@Configuration
class RateLimiterConfig {

    /**
     * 요청 헤더 "X-Client-Id" 값을 Rate Limiting Key로 사용합니다.
     */
    @Bean
    fun clientIdKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            // 요청 헤더에서 "X-Client-Id" 값을 찾고, 없으면 "anonymous"를 기본값으로 사용합니다.
            Mono.justOrEmpty(
                exchange.request.headers.getFirst("X-Client-Id")
            ).defaultIfEmpty("anonymous")
        }
    }
}