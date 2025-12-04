package com.naver.pay.controller.v1

data class ShortUrlStateResponseDto (
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val createdAt: String,
    val expiresAt: String,
    val clickSummary: ClickSummaryResponseDto
)

data class ClickSummaryResponseDto (
    val totalClicks: Long,
    val lastClickedAt: String?,
)

data class ShortUrlStatisticsResponseDto (
    val shortKey: String,
    val totalClicks: Long,
    val byDate: List<ShortUrlStatisticsByDateResponseDto>,
    val byDevice: List<ShortUrlStatisticsByDeviceTypeResponseDto>,
    val byReferrer: List<ShortUrlStatisticsByReferrerResponseDto>
)

data class ShortUrlStatisticsByDateResponseDto (
    val date: String,
    val clicks: Long,
)

data class ShortUrlStatisticsByDeviceTypeResponseDto (
    val deviceType: String,
    val clicks: Long,
)

data class ShortUrlStatisticsByReferrerResponseDto (
    val referrer: String,
    val clicks: Long,
)