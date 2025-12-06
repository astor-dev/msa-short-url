@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.naver.pay.shorturl

import java.time.Instant
import java.util.UUID
import kotlin.io.encoding.Base64

class ShortUrl private constructor(
    val id: Long?,
    val shortKey: String,
    val baseUrl: String,
    val originalUrl: String,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant,
) {
    companion object {
        fun generate(
            shortKey: String,
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
            shortKey: String,
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

        /**
         * shortKey를 생성합니다.
         * 
         * id가 존재하는 경우 BASE64 전략을 사용하고, 존재하지 않는 경우 UUID 전략을 사용합니다.
         * 
         * @param id ShortUrl의 id (null인 경우 UUID 전략 사용)
         * @return 생성된 shortKey
         */
        fun generateShortKey(id: Long?): String {
            return if (id != null) {
                // id가 존재하는 경우: BASE64 전략
                Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(id.toString().toByteArray())
            } else {
                // id가 존재하지 않는 경우: UUID 전략
                UUID.randomUUID().toString().replace("-", "")
            }
        }
    }

    init {
        require(baseUrl.isNotBlank()) { "baseUrl 비어 있을 수 없습니다." }
        require(originalUrl.isNotBlank()) { "originalUrl는 비어 있을 수 없습니다." }
        require(shortKey.isNotBlank()) { "shortKey는 비어 있을 수 없습니다." }
        require(expiresAt.isAfter(createdAt)) { "expiresAt은 createdAt 이후여야 합니다." }
    }

    fun generateShortUrlOrThrow(): String {
        return "${this.baseUrl}/$shortKey"
    }
}