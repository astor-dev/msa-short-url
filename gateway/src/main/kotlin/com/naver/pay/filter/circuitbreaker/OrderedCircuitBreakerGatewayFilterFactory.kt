package com.naver.pay.filter.circuitbreaker

import com.naver.pay.filter.GatewayFilterOrder
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory
import org.springframework.stereotype.Component
import kotlin.jvm.java

@Component("OrderedCircuitBreaker")
class OrderedCircuitBreakerGatewayFilterFactory(
    private val circuitBreakerFactory: SpringCloudCircuitBreakerFilterFactory
) : AbstractGatewayFilterFactory<SpringCloudCircuitBreakerFilterFactory.Config>(
    SpringCloudCircuitBreakerFilterFactory.Config::class.java,
) {
    override fun apply(config: SpringCloudCircuitBreakerFilterFactory.Config): GatewayFilter {
        val circuitBreakerFilter = circuitBreakerFactory.apply(config)
        return OrderedGatewayFilter(circuitBreakerFilter, GatewayFilterOrder.CIRCUIT_BREAKER)
    }
}

