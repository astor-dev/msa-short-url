package com.naver.pay.shorturl

import java.time.Instant

data class ShortUrlCreatedPayload (
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val shortUrlCreatedAt: Instant,
    val shortUrlExpiredAt: Instant
)