package com.naver.pay.shorturl.stats

import java.time.Instant

data class ShortUrlTotalStats (
    val shortKey: String,
    val totalClicks: Long,
    val byDate: List<ShortUrlStatsByDate>,
    val byDevice: List<ShortUrlStatsByDevice>,
    val byReferrer: List<ShortUrlStatsByReferrer>,
    val lastClickedAt: Instant?,
    val metadata: ShortUrlMetadata
)

data class ShortUrlStatsByDate (
    val date: String,
    val clicks: Long
)

data class ShortUrlStatsByDevice (
    val deviceType: String,
    val clicks: Long
)

data class ShortUrlStatsByReferrer (
    val referrer: String,
    val clicks: Long
)