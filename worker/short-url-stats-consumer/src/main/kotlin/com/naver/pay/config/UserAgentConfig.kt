package com.naver.pay.config

import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserAgentConfig {

    @Bean
    fun userAgentAnalyzer(): UserAgentAnalyzer {
        return UserAgentAnalyzer
            .newBuilder()
            .hideMatcherLoadStats() // 시작 시 수천 줄의 로그 출력 방지
            .withCache(10000)       // 자주 들어오는 User-Agent 캐싱 (성능 핵심)
            .withField("DeviceClass")
            .withField("AgentName")
            .build()
    }
}