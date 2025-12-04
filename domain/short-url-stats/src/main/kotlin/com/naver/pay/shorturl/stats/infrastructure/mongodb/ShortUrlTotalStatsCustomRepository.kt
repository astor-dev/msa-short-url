package com.naver.pay.shorturl.stats.infrastructure.mongodb

import java.time.Instant

interface ShortUrlTotalStatsCustomRepository {
    fun recordClickAtomically(shortKey: String, referrer: String, device: String, date: String, clickedAt: Instant)
}