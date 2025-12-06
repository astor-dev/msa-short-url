package com.naver.pay.shorturl

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.outbox.OutboxService
import com.naver.pay.shorturl.jpa.ShortUrlEntity
import com.naver.pay.shorturl.jpa.ShortUrlJpaRepository
import com.naver.pay.shorturl.stream.Bindings
import com.naver.pay.shorturl.stream.ShortUrlCreatedPayload
import jakarta.transaction.Transactional
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

@Repository
open class ShortUrlRepository(
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
        val cacheKey = "${CacheNames.SHORT_URL_BY_SHORT_KEY}::${shortUrl.shortKey}"
        val jsonString = objectMapper.writeValueAsString(shortUrl)
        redisTemplate.opsForValue().set(cacheKey, jsonString, ttl)
    }

    /**
     * 짧은 URL을 생성하고 저장합니다.
     * 
     * DB의 shortKey 컬럼은 non-null 및 unique 제약조건을 가지고 있어, 저장 시점에 유효한 값이 필요합니다.
     * 하지만 최종 shortKey는 DB에서 생성된 id를 기반으로 생성해야 하므로, 두 단계 저장 전략을 사용합니다:
     * 1. id 없을 때 전략으로 임시 shortKey 생성하여 초기 저장 (DB 제약조건 충족 및 id 생성)
     * 2. 생성된 id 있을 때 전략으로 최종 shortKey 생성하여 업데이트 (최종 shortKey 확정)
     * UUID 기반 전략은 shortKey가 길어 ‘짧은 URL’ 의 목적을 충족하지 못하므로,
     * id 기반으로 짧고 충돌 가능성이 낮은 shortKey를 생성하기 위해 두 단계 저장이 필요합니다.
     * shortKey 충돌이 발생할 경우 최대 3회까지 재시도하며, id 있을 때 전략은 충돌 가능성이 없습니다.
     * outbox에 SHORT_URL_CREATED 이벤트를 발행합니다.
     * 
     * @param baseUrl 짧은 URL의 기본 URL
     * @param originalUrl 원본 URL
     * @param ttlSeconds URL의 유효 기간(초)
     * @return 생성된 ShortUrl 객체
     * @throws DataIntegrityViolationException 최대 재시도 횟수를 초과한 경우
     */
    @Transactional
    open fun createShortUrl(
        baseUrl: String,
        originalUrl: String,
        ttlSeconds: Int
    ): ShortUrl {
        // 1단계: id 없을 때 전략으로 임시 shortKey 생성하여 저장 (DB non-null 및 unique 제약조건 충족 + id 생성)
        val entityWithGeneratedId = saveEntityWithTemporaryShortKey(
            baseUrl = baseUrl,
            originalUrl = originalUrl,
            ttlSeconds = ttlSeconds
        )
        
        // 2단계: id 있을 때 전략으로 최종 shortKey 생성하여 업데이트 (최종 shortKey 확정)
        val finalShortKeyWithIdStrategy = ShortUrl.generateShortKey(id = entityWithGeneratedId.id)
        entityWithGeneratedId.shortKey = finalShortKeyWithIdStrategy
        val entityWithFinalShortKey = shortUrlJpaRepository.save(entityWithGeneratedId)
        val finalShortUrl = entityWithFinalShortKey.toDomain()

        // 3단계: 이벤트 발행
        outboxService.storeEvent(Bindings.SHORT_URL_CREATED, ShortUrlCreatedPayload(
            shortKey = finalShortUrl.shortKey,
            originalUrl = finalShortUrl.originalUrl,
            shortUrl = finalShortUrl.generateShortUrlOrThrow(),
            shortUrlCreatedAt = finalShortUrl.createdAt,
            shortUrlExpiredAt = finalShortUrl.expiresAt
        ))
        
        return finalShortUrl
    }

    /**
     * id 없을 때 전략으로 임시 shortKey를 생성하여 엔티티를 저장합니다.
     * 
     * DB의 shortKey 컬럼은 non-null 및 unique 제약조건을 가지고 있어, 저장 시점에 유효한 값이 필요합니다.
     * 하지만 이 시점에는 아직 DB에서 id가 생성되지 않았으므로, id 없을 때 전략을 사용하여 임시 shortKey를 생성합니다.
     *
     * shortKey 충돌이 발생할 경우 최대 3회까지 재시도합니다.
     * 
     * @param baseUrl 짧은 URL의 기본 URL
     * @param originalUrl 원본 URL
     * @param ttlSeconds URL의 유효 기간(초)
     * @return id가 생성된 저장된 ShortUrlEntity
     * @throws DataIntegrityViolationException 최대 재시도 횟수를 초과한 경우
     */
    private fun saveEntityWithTemporaryShortKey(
        baseUrl: String,
        originalUrl: String,
        ttlSeconds: Int
    ): ShortUrlEntity {
        val maxRetriesForShortKeyCollision = 3
        
        return (1..maxRetriesForShortKeyCollision).firstNotNullOfOrNull { retryAttempt ->
            try {
                val temporaryShortKeyWithoutId = ShortUrl.generateShortKey(id = null)
                val shortUrlWithTemporaryKey = ShortUrl.generate(
                    shortKey = temporaryShortKeyWithoutId,
                    baseUrl = baseUrl,
                    originalUrl = originalUrl,
                    ttlSeconds = ttlSeconds
                )
                shortUrlJpaRepository.save(ShortUrlEntity.of(shortUrlWithTemporaryKey))
            } catch (e: DataIntegrityViolationException) {
                if (retryAttempt == maxRetriesForShortKeyCollision) {
                    throw DataIntegrityViolationException(
                        "shortKey 생성 실패: 최대 재시도 횟수($maxRetriesForShortKeyCollision)를 초과했습니다. 원인: ${e.message}",
                        e.cause
                    )
                }
                null
            }
        } ?: throw IllegalStateException("엔티티 저장에 실패했습니다.")
    }

    fun findByShortKey(shortKey: String): ShortUrlEntity? {
        return shortUrlJpaRepository.findByShortKey(shortKey).getOrNull()
    }

    fun findByOriginalUrl(shortKey: String): ShortUrlEntity? {
        return shortUrlJpaRepository.findByOriginalUrl(shortKey).getOrNull()
    }
}