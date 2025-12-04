    package com.naver.pay.shorturl.stats

data class DailyTopStats(
    val date: String,
    val topUrls: List<TopUrlInfo>,
    val topReferrers: List<TopReferrerInfo>,
    val topByDevice: List<TopByDeviceInfo>
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

