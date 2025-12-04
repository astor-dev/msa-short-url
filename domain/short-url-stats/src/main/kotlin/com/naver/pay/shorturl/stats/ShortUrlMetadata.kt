package com.naver.pay.shorturl.stats

import java.time.Instant

data class ShortUrlMetadata(
    val shortUrl: String,
    val originalUrl: String,
    val shortUrlCreatedAt: Instant,
    val shortUrlExpiredAt: Instant
)