package com.naver.pay.shorturl.stats

data class DailyStatsVo(
    val dateKey: String,
    val topUrls: List<KeyCountVo>,
    val topReferrers: List<KeyCountVo>,
    val topByDevice: List<DeviceStatsVo>
)

data class DeviceStatsVo(
    val deviceType: String,
    val totalCount: Long,
    val topUrls: List<KeyCountVo>
)

data class KeyCountVo(
    val key: String,
    val count: Long
)