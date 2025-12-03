package com.naver.pay.shorturl

data class ShortUrlCreatedPayload (
    val shortKey: String,
    val originalUrl: String
)