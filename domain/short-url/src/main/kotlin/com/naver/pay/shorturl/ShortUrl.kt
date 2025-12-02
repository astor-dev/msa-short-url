package com.naver.pay.shorturl

import java.time.Instant

class ShortUrl private constructor(
    val id: Long?,
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
            return ShortUrl(
                id= null,
                shortKey = shortKey,
                shortUrl = shortUrl,
                originalUrl = originalUrl,
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
        }

        fun of(
            id: Long,
            shortKey: String,
            shortUrl: String,
            originalUrl: String,
            createdAt: Instant,
            expiresAt: Instant,
        ): ShortUrl {
            return ShortUrl(
                id= id,
                shortKey = shortKey,
                shortUrl = shortUrl,
                originalUrl = originalUrl,
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
        }
    }

    init {
        require(shortKey.isNotBlank()) { "shortKey는 비어 있을 수 없습니다." }
        require(shortUrl.isNotBlank()) { "shortUrl는 비어 있을 수 없습니다." }
        require(originalUrl.isNotBlank()) { "originalUrl는 비어 있을 수 없습니다." }
        require(shortUrl.substringAfterLast("/") == shortKey) { "shortUrl의 마지막 경로 식별자는 shortKey와 동일해야 합니다." }
        require(expiresAt.isAfter(createdAt)) { "expiresAt은 createdAt 이후여야 합니다." }
    }
}