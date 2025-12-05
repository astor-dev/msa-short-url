package com.naver.pay.shorturl

import java.time.Instant
import kotlin.io.encoding.Base64

class ShortUrl private constructor(
    val id: Long?,
    val shortKey: String?,
    val baseUrl: String,
    val originalUrl: String,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant,
) {
    companion object {
        fun generate(
            shortKey: String?,
            baseUrl: String,
            originalUrl: String,
            ttlSeconds: Int,
        ): ShortUrl {
            val createdAt = Instant.now()
            val expiresAt = createdAt.plusSeconds(ttlSeconds.toLong())
            return ShortUrl(
                id= null,
                shortKey = shortKey,
                baseUrl = baseUrl,
                originalUrl = originalUrl,
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
        }

        fun of(
            id: Long,
            shortKey: String?,
            baseUrl: String,
            originalUrl: String,
            createdAt: Instant,
            expiresAt: Instant,
        ): ShortUrl {
            return ShortUrl(
                id= id,
                shortKey = shortKey,
                baseUrl = baseUrl,
                originalUrl = originalUrl,
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
        }
    }

    init {
        require(baseUrl.isNotBlank()) { "baseUrl 비어 있을 수 없습니다." }
        require(originalUrl.isNotBlank()) { "originalUrl는 비어 있을 수 없습니다." }
        require(expiresAt.isAfter(createdAt)) { "expiresAt은 createdAt 이후여야 합니다." }
    }

    fun generateShortKeyFromId(): ShortUrl {
        val shortKey = id?.let { Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(it.toString().toByteArray()) }
            ?: throw IllegalStateException("id가 설정되어 있지 않습니다.")
        return ShortUrl(
            id = this.id,
            shortKey = shortKey,
            baseUrl = this.baseUrl,
            originalUrl = this.originalUrl,
            createdAt = this.createdAt,
            expiresAt = this.expiresAt,
        )
    }

    fun generateShortUrlOrThrow(): String {
        val key = checkNotNull(this.shortKey) { "shortKey가 설정되어 있지 않습니다." }
        return "${this.baseUrl}/$key"
    }
}