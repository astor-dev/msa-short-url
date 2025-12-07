package com.naver.pay.shorturl

import com.naver.pay.outbox.OutboxService
import com.naver.pay.shorturl.jpa.ShortUrlEntity
import com.naver.pay.shorturl.jpa.ShortUrlJpaRepository
import com.naver.pay.shorturl.stream.Bindings
import com.naver.pay.shorturl.stream.ShortUrlCreatedPayload
import com.naver.pay.shorturl.stream.ShortUrlEventProducer
import com.naver.pay.util.DistributedLockExecutor
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import kotlin.jvm.optionals.getOrNull

@Repository
class ShortUrlRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val outboxService: OutboxService,
    private val shortUrlJpaRepository: ShortUrlJpaRepository,
    private val distributedLockExecutor: DistributedLockExecutor,
    private val shortUrlEventProducer: ShortUrlEventProducer
) {
    /**
     * shortKey로 RedirectUrl 도메인 객체를 조회합니다.
     * 
     * 캐시에서 redirectUrl과 expiresAt을 조회하고, 없을 경우 DB에서 조회 후 캐시에 저장합니다.
     * Cache Stampede 방지를 위해 Jitter(0~60분)를 추가하여 캐싱합니다.
     * Thundering herd 방지를 위해 조회 로직에 분산락을 사용합니다.
     * 
     * @param shortKey 조회할 ShortUrl의 shortKey
     * @param userAgent 사용자 에이전트 (이벤트 발행용)
     * @param referrer 리퍼러 (이벤트 발행용)
     * @return 조회된 RedirectUrl 도메인 객체, 없으면 null
     */
    fun getRedirectUrl(shortKey: String, userAgent: String?, referrer: String?): RedirectUrl? {
        // 1. 캐시에서 redirectUrl과 expiresAt 조회
        val cachedRedirectUrl = findRedirectUrlInCache(shortKey)
        val cachedExpiresAt = findExpiresAtInCache(shortKey)
        // 2. 둘 다 캐시에 있으면 RedirectUrl 도메인 객체 생성하여 반환
        val redirectUrl = if (cachedRedirectUrl != null && cachedExpiresAt != null) {
            RedirectUrl(url = cachedRedirectUrl, expiresAt = cachedExpiresAt)
        } else {
            distributedLockExecutor.execute(lockName = CacheNames.SHORT_URL_GET_LOCK, key = shortKey) {
                // 3-1. 락 획득 후 캐시 재확인
                val redirectUrl = findRedirectUrlInCache(shortKey)
                val expiresAt = findExpiresAtInCache(shortKey)

                val resolved = if (redirectUrl != null && expiresAt != null) {
                    RedirectUrl(url = redirectUrl, expiresAt = expiresAt)
                } else {
                    val shortUrlEntity = findByShortKey(shortKey)
                    val redirectUrl = shortUrlEntity?.let { entity ->
                        val redirectUrlValue = entity.originalUrl
                        val expiresAtValue = entity.expiresAt
                        val ttl = calculateDynamicTtl()
                        cacheRedirectUrlAndExpiresAt(shortKey,redirectUrlValue, expiresAtValue, ttl)
                        RedirectUrl(
                            url = redirectUrlValue,
                            expiresAt = expiresAtValue
                        )
                    }
                    redirectUrl
                }
                resolved
            }
        }
        if(redirectUrl != null) {
            shortUrlEventProducer.publishUrlClicked(
                shortKey = shortKey,
                userAgent = userAgent ?: "Unknown",
                referrer = referrer ?: "Direct",
            )
        }
        return redirectUrl
    }

    /**
     * 캐시에서 redirectUrl을 조회합니다.
     * 
     * @param shortKey 조회할 ShortUrl의 shortKey
     * @return 캐시된 redirectUrl, 없으면 null
     */
    private fun findRedirectUrlInCache(shortKey: String): String? {
        val cacheKey = "${CacheNames.REDIRECT_URL_BY_SHORT_KEY}::$shortKey"
        return redisTemplate.opsForValue().get(cacheKey)
    }

    /**
     * 캐시에서 expiresAt을 조회합니다.
     * 
     * @param shortKey 조회할 ShortUrl의 shortKey
     * @return 캐시된 expiresAt, 없으면 null
     */
    private fun findExpiresAtInCache(shortKey: String): Instant? {
        val cacheKey = "${CacheNames.EXPIRES_AT_BY_SHORT_KEY}::$shortKey"
        val expiresAtString = redisTemplate.opsForValue().get(cacheKey)
        return expiresAtString?.let { Instant.parse(it) }
    }

    /**
     * redirectUrl과 expiresAt을 캐시에 저장합니다.
     * 
     * @param shortKey ShortUrl의 shortKey
     * @param redirectUrl 저장할 redirectUrl
     * @param expiresAt 저장할 expiresAt
     * @param ttl 캐시 TTL
     */
    private fun cacheRedirectUrlAndExpiresAt(shortKey: String, redirectUrl: String, expiresAt: Instant, ttl: Duration) {
        val redirectUrlCacheKey = "${CacheNames.REDIRECT_URL_BY_SHORT_KEY}::$shortKey"
        val expiresAtCacheKey = "${CacheNames.EXPIRES_AT_BY_SHORT_KEY}::$shortKey"
        redisTemplate.opsForValue().set(redirectUrlCacheKey, redirectUrl, ttl)
        redisTemplate.opsForValue().set(expiresAtCacheKey, expiresAt.toString(), ttl)
    }

    /**
     * 기본 TTL(2주) + Jitter(0~60분)을 적용한 Duration을 계산합니다.
     * 
     * @return 계산된 TTL Duration
     */
    private fun calculateDynamicTtl(): Duration {
        val ttlSeconds = 60 * 60 * 24 * 14  // 기본 2주
        val jitterRangeSeconds = 3600L // Jitter 범위 1시간
        val jitter = ThreadLocalRandom.current().nextLong(jitterRangeSeconds)
        val standardTtlSeconds = ttlSeconds + jitter
        return Duration.ofSeconds(standardTtlSeconds)
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
    fun createShortUrl(
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

    fun findByOriginalUrl(originalUrl: String): ShortUrlEntity? {
        return shortUrlJpaRepository.findByOriginalUrl(originalUrl).getOrNull()
    }

    /**
     * originalUrl로 ShortUrl을 조회합니다.
     * 
     * Cacheable 어노테이션을 사용하여 자동으로 캐싱 처리합니다.
     * 캐시에 없을 경우 DB에서 조회 후 캐시에 저장합니다.
     * 주된 조회 경로가 아니기에 DB 부하 방지 보다는 락 비용이 크다고 여겨 별도의 처리를 하지 않습니다.
     * 
     * @param originalUrl 조회할 ShortUrl의 originalUrl
     * @return 조회된 ShortUrl 도메인 객체, 없으면 null
     */
    @Cacheable(cacheNames = [CacheNames.SHORT_URL_BY_ORIGINAL], key = "#originalUrl")
    fun findShortUrlByOriginalUrl(originalUrl: String): ShortUrl? {
        return findByOriginalUrl(originalUrl)?.toDomain()
    }
}