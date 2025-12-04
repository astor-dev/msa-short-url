package com.naver.pay.service

import com.naver.pay.controller.v1.ClickSummaryResponseDto
import com.naver.pay.controller.v1.DailyTopStatsResponseDto
import com.naver.pay.controller.v1.ShortUrlStateResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsByDateResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsByDeviceTypeResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsByReferrerResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsResponseDto
import com.naver.pay.controller.v1.TopByDeviceResponseDto
import com.naver.pay.controller.v1.TopByDeviceUrlResponseDto
import com.naver.pay.controller.v1.TopReferrerResponseDto
import com.naver.pay.controller.v1.TopUrlResponseDto
import com.naver.pay.shorturl.resolved.ResolvedShortUrl
import com.naver.pay.shorturl.stats.ShortUrlDailyTopStats
import com.naver.pay.shorturl.stats.ShortUrlTotalStats
import java.time.temporal.ChronoUnit

fun ResolvedShortUrl.toDto(): ShortUrlStateResponseDto {
    return ShortUrlStateResponseDto(
        shortKey = this.shortKey,
        shortUrl = this.shortUrl,
        originalUrl = this.originalUrl,
        createdAt = this.createdAt.truncatedTo(ChronoUnit.SECONDS).toString(),
        expiresAt = this.expiredAt.truncatedTo(ChronoUnit.SECONDS).toString(),
        ClickSummaryResponseDto(
            totalClicks = this.clickSummary.totalClicks,
            lastClickedAt = this.clickSummary.lastClickedAt?.truncatedTo(ChronoUnit.SECONDS)?.toString(),
        )
    )
}

fun ShortUrlTotalStats.toDto() : ShortUrlStatisticsResponseDto {
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

fun ShortUrlDailyTopStats.toDto(): DailyTopStatsResponseDto {
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