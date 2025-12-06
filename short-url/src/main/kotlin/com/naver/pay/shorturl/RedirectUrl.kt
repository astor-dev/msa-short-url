package com.naver.pay.shorturl

import java.time.Instant

data class RedirectUrl (
    val url: String,
    val expiresAt: Instant
)