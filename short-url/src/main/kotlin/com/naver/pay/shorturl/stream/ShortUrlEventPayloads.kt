package com.naver.pay.shorturl.stream

import java.time.Instant

data class ShortUrlClickedPayload (
    val shortKey: String,
    val referrer: String = "Direct",
    val userAgent: String = "Unknown",
    val clickedAt: Instant
)

data class ShortUrlCreatedPayload (
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val shortUrlCreatedAt: Instant,
    val shortUrlExpiredAt: Instant
)