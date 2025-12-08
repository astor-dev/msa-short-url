package com.naver.pay.traffic.client.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateUrlRequest(
    val originalUrl: String,
    val ttlSeconds: Long
)

