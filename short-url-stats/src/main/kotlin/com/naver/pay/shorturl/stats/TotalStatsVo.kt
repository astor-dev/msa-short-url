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

