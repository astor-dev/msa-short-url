package com.naver.pay.controller.v1

import jakarta.validation.constraints.Min
import org.hibernate.validator.constraints.URL

data class UrlRequestDto(
    @field:URL(message = "올바른 URL 형식이어야 합니다.")
    val originalUrl: String,

    @field:Min(value = 1, message = "ttl은 최소 1초 이상이어야 합니다.")
    val ttlSeconds: Int
)

data class UrlResponseDto(
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val createdAt: String,
    val expiresAt: String
)