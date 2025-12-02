package com.naver.pay.shorturl

import java.time.Instant

class ShortUrl private constructor(
    val shortKey: String,
    val shortUrl: String,
    val originalUrl: String,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant,
) {
    companion object {
        fun of(
            shortKey: String,
            shortUrl: String,
            originalUrl: String,
            ttlSeconds: Int,
        ): ShortUrl {
            val createdAt = Instant.now()
            val expiresAt = createdAt.plusSeconds(ttlSeconds.toLong())

            require(shortKey.isNotBlank()) { "shortKey는 비어 있을 수 없습니다." }
            require(shortUrl.isNotBlank()) { "shortUrl는 비어 있을 수 없습니다." }
            require(originalUrl.isNotBlank()) { "originalUrl는 비어 있을 수 없습니다." }
            require(shortUrl.substringAfterLast("/") == shortKey) { "shortUrl의 마지막 경로 식별자는 shortKey와 동일해야 합니다." }
            require(ttlSeconds > 0) { "ttlSeconds는 0보다 커야 합니다." }

            return ShortUrl(
                shortKey = shortKey,
                shortUrl = shortUrl,
                originalUrl = originalUrl,
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
        }
    }
}