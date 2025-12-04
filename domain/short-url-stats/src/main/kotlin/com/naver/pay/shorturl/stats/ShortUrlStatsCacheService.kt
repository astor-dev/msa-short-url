package com.naver.pay.shorturl.stats

interface ShortUrlStatsCacheService {
    fun recordClickAtomically(
        dateKey: String,
        shortKey: String,
        referrer: String,
        device: String
    )
}