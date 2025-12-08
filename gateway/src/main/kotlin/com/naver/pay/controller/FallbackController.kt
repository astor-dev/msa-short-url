package com.naver.pay.controller

import com.naver.pay.response.ErrorResponseUtil
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/fallback")
class FallbackController {

    @GetMapping("/redirect")
    fun redirectFallback(exchange: ServerWebExchange): Mono<Void> {
        val path = exchange.request.path.value()
        return ErrorResponseUtil.createServiceUnavailableResponse(
            exchange = exchange,
            message = "Short URL service is temporarily unavailable. Please try again later.",
            path = path
        )
    }

    @PostMapping("/create")
    fun createFallback(exchange: ServerWebExchange): Mono<Void> {
        val path = exchange.request.path.value()
        return ErrorResponseUtil.createServiceUnavailableResponse(
            exchange = exchange,
            message = "Short URL creation service is temporarily unavailable. Please retry after a moment.",
            path = path
        )
    }

    @GetMapping("/state")
    fun stateFallback(exchange: ServerWebExchange): Mono<Void> {
        val path = exchange.request.path.value()
        return ErrorResponseUtil.createServiceUnavailableResponse(
            exchange = exchange,
            message = "Statistics service is temporarily unavailable. Please try again later.",
            path = path
        )
    }

    @GetMapping("/statistics")
    fun statisticsFallback(exchange: ServerWebExchange): Mono<Void> {
        val path = exchange.request.path.value()
        return ErrorResponseUtil.createServiceUnavailableResponse(
            exchange = exchange,
            message = "Statistics service is temporarily unavailable. Please try again later.",
            path = path
        )
    }
}

