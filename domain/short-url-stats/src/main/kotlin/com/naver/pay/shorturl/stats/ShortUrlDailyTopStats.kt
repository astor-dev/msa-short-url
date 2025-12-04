    package com.naver.pay.shorturl.stats

data class ShortUrlDailyTopStats(
    val date: String,
    val topUrls: List<TopUrlInfo> = emptyList(),
    val topReferrers: List<TopReferrerInfo> = emptyList(),
    val topByDevice: List<TopByDeviceInfo> = emptyList(),
)

data class TopUrlInfo(
    val rank: Int,
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val totalClicks: Long
)

data class TopReferrerInfo(
    val rank: Int,
    val referrer: String,
    val totalClicks: Long
)

data class TopByDeviceInfo(
    val deviceType: String,
    val totalClicks: Long,
    val topUrls: List<TopUrlInfo>
)

