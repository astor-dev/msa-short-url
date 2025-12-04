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