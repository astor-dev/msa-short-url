package com.naver.pay.shorturl.resolved

import java.time.Instant

data class ResolvedShortUrl (
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val createdAt: Instant,
    val expiredAt: Instant,
    val clickSummary: ClickSummary
)
