package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsDocument

/**
 * Map<String, String>을 List<DateCountVo>로 변환합니다.
 */
fun Map<String, String>.toDateCountVoList(): List<DateCountVo> {
    return this.map { (date, clicks) ->
        DateCountVo(
            date = date,
            clicks = clicks.toLongOrNull() ?: 0L
        )
    }
}

/**
 * Map<String, String>을 List<DeviceCountVo>로 변환합니다.
 */
fun Map<String, String>.toDeviceCountVoList(): List<DeviceCountVo> {
    return this.map { (deviceType, clicks) ->
        DeviceCountVo(
            deviceType = deviceType,
            clicks = clicks.toLongOrNull() ?: 0L
        )
    }
}

/**
 * Map<String, String>을 List<ReferrerCountVo>로 변환합니다.
 */
fun Map<String, String>.toReferrerCountVoList(): List<ReferrerCountVo> {
    return this.map { (referrer, clicks) ->
        ReferrerCountVo(
            referrer = referrer,
            clicks = clicks.toLongOrNull() ?: 0L
        )
    }
}

/**
 * DateCountVo를 ShortUrlStatsByDate로 변환합니다.
 */
fun DateCountVo.toDomain(): ShortUrlStatsByDate {
    return ShortUrlStatsByDate(
        date = this.date,
        clicks = this.clicks
    )
}

/**
 * DeviceCountVo를 ShortUrlStatsByDevice로 변환합니다.
 */
fun DeviceCountVo.toDomain(): ShortUrlStatsByDevice {
    return ShortUrlStatsByDevice(
        deviceType = this.deviceType,
        clicks = this.clicks
    )
}

/**
 * ReferrerCountVo를 ShortUrlStatsByReferrer로 변환합니다.
 */
fun ReferrerCountVo.toDomain(): ShortUrlStatsByReferrer {
    return ShortUrlStatsByReferrer(
        referrer = this.referrer,
        clicks = this.clicks
    )
}

/**
 * TotalStatsVo와 ShortUrlTotalStatsDocument를 TotalStats로 변환합니다.
 */
fun TotalStatsVo.toDomain(document: ShortUrlTotalStatsDocument): TotalStats {
    return TotalStats(
        shortKey = this.shortKey,
        totalClicks = this.totalClicks,
        byDate = this.byDate.map { it.toDomain() },
        byDevice = this.byDevice.map { it.toDomain() },
        byReferrer = this.byReferrer.map { it.toDomain() },
        lastClickedAt = this.lastClickedAt,
        metadata = document.metadata
    )
}
