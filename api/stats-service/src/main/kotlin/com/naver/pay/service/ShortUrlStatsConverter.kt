package com.naver.pay.service

import com.naver.pay.controller.v1.ClickSummaryResponseDto
import com.naver.pay.controller.v1.ShortUrlStateResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsByDateResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsByDeviceTypeResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsByReferrerResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsResponseDto
import com.naver.pay.shorturl.resolved.ResolvedShortUrl
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