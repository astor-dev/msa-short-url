package com.naver.pay.payload

import java.time.Instant

data class ShortUrlCreatedPayload (
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val shortUrlCreatedAt: Instant,
    val shortUrlExpiredAt: Instant
)