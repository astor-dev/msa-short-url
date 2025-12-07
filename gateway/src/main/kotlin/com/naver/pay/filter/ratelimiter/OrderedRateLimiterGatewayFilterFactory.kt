package com.naver.pay.filter.ratelimiter

import com.naver.pay.filter.GatewayFilterOrder
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory
import org.springframework.stereotype.Component

@Component("OrderedRateLimiter")
class OrderedRateLimiterGatewayFilterFactory(
    private val rateLimiterFactory: RequestRateLimiterGatewayFilterFactory
) : AbstractGatewayFilterFactory<RequestRateLimiterGatewayFilterFactory.Config>(
    RequestRateLimiterGatewayFilterFactory.Config::class.java,
) {
    override fun apply(config: RequestRateLimiterGatewayFilterFactory.Config): GatewayFilter {
        val rateLimiterFilter = rateLimiterFactory.apply(config)
        return OrderedGatewayFilter(rateLimiterFilter, GatewayFilterOrder.RATE_LIMITING)
    }
}