package com.naver.pay.service

import com.naver.pay.controller.v1.ClickSummaryResponseDto
import com.naver.pay.controller.v1.ShortUrlStateResponseDto
import com.naver.pay.shorturl.resolved.ResolvedShortUrl
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