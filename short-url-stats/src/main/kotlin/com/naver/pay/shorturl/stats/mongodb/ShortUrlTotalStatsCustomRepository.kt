package com.naver.pay.shorturl.stats.mongodb

import java.time.Instant
import java.time.LocalDate

interface ShortUrlTotalStatsCustomRepository {
    fun recordClickAtomically(shortKey: String, referrer: String, device: String, date: LocalDate, clickedAt: Instant)
}