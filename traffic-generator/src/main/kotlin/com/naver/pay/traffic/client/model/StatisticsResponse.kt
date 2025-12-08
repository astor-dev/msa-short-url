package com.naver.pay.traffic.client.model

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsResponse(
    val shortKey: String,
    val totalClicks: Long,
    val byDate: List<DateClicks>,
    val byDevice: List<DeviceClicks>,
    val byReferrer: List<ReferrerClicks>
)

@Serializable
data class DateClicks(
    val date: String,
    val clicks: Long
)

@Serializable
data class DeviceClicks(
    val deviceType: String,
    val clicks: Long
)

@Serializable
data class ReferrerClicks(
    val referrer: String,
    val clicks: Long
)

