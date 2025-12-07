package com.naver.pay.shorturl.stats

import java.time.Instant

data class TotalStatsVo(
    val shortKey: String,
    val totalClicks: Long,
    val byDate: List<DateCountVo>,
    val byDevice: List<DeviceCountVo>,
    val byReferrer: List<ReferrerCountVo>,
    val lastClickedAt: Instant?
)

data class DateCountVo(
    val date: String,
    val clicks: Long
)

data class DeviceCountVo(
    val deviceType: String,
    val clicks: Long
)

data class ReferrerCountVo(
    val referrer: String,
    val clicks: Long
)

data class TotalStatsCacheKeys(
    val totalClicksKey: String,
    val byDateKey: String,
    val byDeviceKey: String,
    val byReferrerKey: String,
    val lastClickedAtKey: String,
    val dirtySetKey: String
) {
    companion object {
        fun from(shortKey: String): TotalStatsCacheKeys {
            return TotalStatsCacheKeys(
                totalClicksKey = "${CacheNames.TOTAL_STATS_TOTAL_CLICKS}::$shortKey",
                byDateKey = "${CacheNames.TOTAL_STATS_BY_DATE}::$shortKey",
                byDeviceKey = "${CacheNames.TOTAL_STATS_BY_DEVICE}::$shortKey",
                byReferrerKey = "${CacheNames.TOTAL_STATS_BY_REFERRER}::$shortKey",
                lastClickedAtKey = "${CacheNames.TOTAL_STATS_LAST_CLICKED_AT}::$shortKey",
                dirtySetKey = CacheNames.TOTAL_STATS_DIRTY_SET
            )
        }
    }
}

