package com.naver.pay.traffic.client.model

import kotlinx.serialization.Serializable

@Serializable
data class TopStatisticsResponse(
    val date: String,
    val topUrls: List<TopUrl>,
    val topReferrers: List<TopReferrer>,
    val topByDevice: List<TopByDevice>
)

@Serializable
data class TopUrl(
    val rank: Int,
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val totalClicks: Long
)

@Serializable
data class TopReferrer(
    val rank: Int,
    val referrer: String,
    val totalClicks: Long
)

@Serializable
data class TopByDevice(
    val deviceType: String,
    val totalClicks: Long,
    val topUrls: List<TopUrlByDevice>
)

@Serializable
data class TopUrlByDevice(
    val rank: Int,
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val clicksFromThisDevice: Long
)

