package com.naver.pay.shorturl.stats.infrastructure.mongodb

interface ShortUrlTotalStatsCustomRepository {
    fun recordClickAtomically(shortKey: String, referrer: String, device: String, date: String)
}