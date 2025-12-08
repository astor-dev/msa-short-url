package com.naver.pay.response

import com.naver.pay.util.createCommonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

object ErrorResponseUtil {
    private val objectMapper = createCommonObjectMapper()

    fun createUnauthorizedResponse(
        exchange: ServerWebExchange,
        message: String,
        path: String
    ): Mono<Void> {
        val response: ServerHttpResponse = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.add("Content-Type", "application/json")

        val errorResponse = ErrorResponse(
            timestamp = System.currentTimeMillis().toString(),
            status = HttpStatus.UNAUTHORIZED,
            error = "Unauthorized",
            message = message,
            path = path
        )

        val body = objectMapper.writeValueAsString(errorResponse)
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    fun createForbiddenResponse(
        exchange: ServerWebExchange,
        message: String,
        path: String
    ): Mono<Void> {
        val response: ServerHttpResponse = exchange.response
        response.statusCode = HttpStatus.FORBIDDEN
        response.headers.add("Content-Type", "application/json")

        val errorResponse = ErrorResponse(
            timestamp = System.currentTimeMillis().toString(),
            status = HttpStatus.FORBIDDEN,
            error = "Forbidden",
            message = message,
            path = path
        )

        val body = objectMapper.writeValueAsString(errorResponse)
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    fun createServiceUnavailableResponse(
        exchange: ServerWebExchange,
        message: String,
        path: String
    ): Mono<Void> {
        val response: ServerHttpResponse = exchange.response
        response.statusCode = HttpStatus.SERVICE_UNAVAILABLE
        response.headers.add("Content-Type", "application/json")

        val errorResponse = ErrorResponse(
            timestamp = System.currentTimeMillis().toString(),
            status = HttpStatus.SERVICE_UNAVAILABLE,
            error = "Service Unavailable",
            message = message,
            path = path
        )

        val body = objectMapper.writeValueAsString(errorResponse)
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }
}