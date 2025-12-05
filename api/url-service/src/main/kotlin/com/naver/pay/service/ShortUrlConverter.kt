package com.naver.pay.service

import com.naver.pay.controller.v1.UrlResponseDto
import com.naver.pay.shorturl.ShortUrl
import java.time.temporal.ChronoUnit

fun ShortUrl.toDto(): UrlResponseDto {
    val key = checkNotNull(this.shortKey) { "shortKey가 설정되어 있지 않습니다." }
    return UrlResponseDto(
        originalUrl = this.originalUrl,
        shortUrl = generateShortUrlOrThrow(),
        shortKey = key,
        createdAt = this.createdAt.truncatedTo(ChronoUnit.SECONDS).toString(),
        expiresAt = this.expiresAt.truncatedTo(ChronoUnit.SECONDS).toString(),
    )
}