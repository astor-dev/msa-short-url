package com.naver.pay.controller

import com.naver.pay.shorturl.stats.DailyTopStats
import com.naver.pay.shorturl.stats.TotalStats
import java.time.temporal.ChronoUnit

fun TotalStats.toStateDto() : ShortUrlStateResponseDto {
    return ShortUrlStateResponseDto(
        shortKey = this.shortKey,
        shortUrl = this.metadata.shortUrl,
        originalUrl = this.metadata.originalUrl,
        createdAt = this.metadata.shortUrlCreatedAt.truncatedTo(ChronoUnit.SECONDS).toString(),
        expiresAt = this.metadata.shortUrlExpiredAt.truncatedTo(ChronoUnit.SECONDS).toString(),
        ClickSummaryResponseDto(
            totalClicks = this.totalClicks,
            lastClickedAt = this.lastClickedAt?.truncatedTo(ChronoUnit.SECONDS)?.toString(),
        )
    )
}

fun TotalStats.toStatsDto() : ShortUrlStatisticsResponseDto {
    val byDate = this.byDate.map {
        ShortUrlStatisticsByDateResponseDto(
            date = it.date,
            clicks = it.clicks
        )
    }

    val byDevice = this.byDevice.map {
        ShortUrlStatisticsByDeviceTypeResponseDto(
            deviceType = it.deviceType,
            clicks = it.clicks
        )
    }

    val byReferrer = this.byReferrer.map {
        ShortUrlStatisticsByReferrerResponseDto(
            referrer = it.referrer,
            clicks = it.clicks
        )
    }
    return ShortUrlStatisticsResponseDto(
        shortKey = this.shortKey,
        totalClicks = this.totalClicks,
        byDate =  byDate,
        byDevice = byDevice,
        byReferrer = byReferrer
    )
}

fun DailyTopStats.toDto(): DailyTopStatsResponseDto {
    val topUrls = this.topUrls.map {
        TopUrlResponseDto(
            rank = it.rank,
            shortKey = it.shortKey,
            shortUrl = it.shortUrl,
            originalUrl = it.originalUrl,
            totalClicks = it.totalClicks
        )
    }

    val topReferrers = this.topReferrers.map {
        TopReferrerResponseDto(
            rank = it.rank,
            referrer = it.referrer,
            totalClicks = it.totalClicks
        )
    }

    val topByDevice = this.topByDevice.map { deviceInfo ->
        TopByDeviceResponseDto(
            deviceType = deviceInfo.deviceType,
            totalClicks = deviceInfo.totalClicks,
            topUrls = deviceInfo.topUrls.map { urlInfo ->
                TopByDeviceUrlResponseDto(
                    rank = urlInfo.rank,
                    shortKey = urlInfo.shortKey,
                    shortUrl = urlInfo.shortUrl,
                    originalUrl = urlInfo.originalUrl,
                    clicksFromThisDevice = urlInfo.totalClicks
                )
            }
        )
    }

    return DailyTopStatsResponseDto(
        date = this.date,
        topUrls = topUrls,
        topReferrers = topReferrers,
        topByDevice = topByDevice
    )
}