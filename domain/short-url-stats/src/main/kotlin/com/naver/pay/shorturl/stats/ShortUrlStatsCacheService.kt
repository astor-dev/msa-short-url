package com.naver.pay.shorturl.stats

// TODO: date-key에서 LocalDate로 타입 변경
interface ShortUrlStatsCacheService {
    fun recordClickAtomically(
        dateKey: String,
        shortKey: String,
        referrer: String,
        device: String
    )

    fun getDailyStatistics(dateKey: String, limit: Long): DailyStatsVo
}