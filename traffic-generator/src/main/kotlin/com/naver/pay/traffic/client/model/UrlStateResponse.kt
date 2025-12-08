package com.naver.pay.traffic.client.model

import kotlinx.serialization.Serializable

@Serializable
data class UrlStateResponse(
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val createdAt: String,
    val expiresAt: String,
    val clickSummary: ClickSummary
)

@Serializable
data class ClickSummary(
    val totalClicks: Long,
    val lastClickedAt: String?
)

