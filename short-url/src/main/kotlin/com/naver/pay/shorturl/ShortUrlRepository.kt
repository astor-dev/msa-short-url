package com.naver.pay.shorturl

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.outbox.OutboxService
import com.naver.pay.shorturl.jpa.ShortUrlEntity
import com.naver.pay.shorturl.jpa.ShortUrlJpaRepository
import com.naver.pay.shorturl.stream.Bindings
import com.naver.pay.shorturl.stream.ShortUrlCreatedPayload
import jakarta.transaction.Transactional
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.lang.IllegalStateException
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

@Repository
class ShortUrlRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val outboxService: OutboxService,
    private val shortUrlJpaRepository: ShortUrlJpaRepository
) {
    fun findShortUrlByShortKeyInCache(shortKey: String): ShortUrl? {
        val cacheKey = "${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey"
        val cachedValue = redisTemplate.opsForValue().get(cacheKey)
        val cachedShortUrl = runCatching {
            objectMapper.readValue(cachedValue, ShortUrl::class.java)
        }.getOrNull()
        return cachedShortUrl
    }

    fun cacheShortUrlByShortKey(shortUrl: ShortUrl, ttl: Duration) {
        shortUrl.shortKey?.let {
            val cacheKey = "${CacheNames.SHORT_URL_BY_SHORT_KEY}::$it"
            val jsonString = objectMapper.writeValueAsString(shortUrl)
            redisTemplate.opsForValue().set(cacheKey, jsonString, ttl)
        } ?: throw IllegalStateException("Short Ket is not set: ${shortUrl.id}")
    }

    /**
     * 짧은 URL을 생성하고 저장합니다.
     * outbox에 SHORT_URL_CREATED 이벤트를 발행합니다.
     * @param baseUrl 짧은 URL의 기본 URL
     * @param originalUrl 원본 URL
     * @param ttlSeconds URL의 유효 기간(초)
     * @return 생성된 ShortUrl 객체
     */
    @Transactional
    fun createShortUrl(
        baseUrl: String,
        originalUrl: String,
        ttlSeconds: Int
    ): ShortUrl {
        val noKeyShortUrl = ShortUrl.generate(
            shortKey = null,
            baseUrl = baseUrl,
            originalUrl = originalUrl,
            ttlSeconds = ttlSeconds
        )
        val savedShortUrl = shortUrlJpaRepository.save(ShortUrlEntity.of(noKeyShortUrl)).toDomain()
        val shortUrlWithKey = savedShortUrl.generateShortKeyFromId()
        val updatedShortUrl = shortUrlJpaRepository.save(ShortUrlEntity.of(shortUrlWithKey)).toDomain()

        // NOTE: 논리 상 shortKey는 null이 될 수 없으나, 안전성을 위해 null 체크를 수행합니다.
        updatedShortUrl.shortKey?.let {
            outboxService.storeEvent(Bindings.SHORT_URL_CREATED, ShortUrlCreatedPayload(
                shortKey = it,
                originalUrl = updatedShortUrl.originalUrl,
                shortUrl = updatedShortUrl.generateShortUrlOrThrow(),
                shortUrlCreatedAt = updatedShortUrl.createdAt,
                shortUrlExpiredAt = updatedShortUrl.expiresAt
            )
            )
        } ?: throw IllegalStateException("Short key should not be null after generation.")
        return updatedShortUrl
    }

    fun findByShortKey(shortKey: String): ShortUrlEntity? {
        return shortUrlJpaRepository.findByShortKey(shortKey).getOrNull()
    }

    fun findByOriginalUrl(shortKey: String): ShortUrlEntity? {
        return shortUrlJpaRepository.findByOriginalUrl(shortKey).getOrNull()
    }
}