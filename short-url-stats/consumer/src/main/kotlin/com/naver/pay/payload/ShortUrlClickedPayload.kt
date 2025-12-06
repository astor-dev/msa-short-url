package com.naver.pay.payload

import java.time.Instant

data class ShortUrlClickedPayload (
    val shortKey: String,
    val referrer: String = "Direct",
    val userAgent: String = "Unknown",
    val clickedAt: Instant = Instant.now()
)
