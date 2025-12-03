package com.naver.pay.shorturl

import java.time.Instant

data class ShortUrlClickedPayload (
    val shortKey: String,
    val originalUrl: String,
    val clickedAt: Instant
)