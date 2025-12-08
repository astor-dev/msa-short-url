package com.naver.pay.traffic.client.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateUrlResponse(
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val createdAt: String,
    val expiresAt: String
)

