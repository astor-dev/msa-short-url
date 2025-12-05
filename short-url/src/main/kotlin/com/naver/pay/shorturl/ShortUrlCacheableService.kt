package com.naver.pay.shorturl

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

@Service
open class ShortUrlCacheableService(
    private val shortUrlRepository: ShortUrlRepository,
) {

    /**
     * 캐시에서 shortKey로 ShortUrl을 조회합니다.
     * 24시간 기본 TTL에 Jitter(0~60분)를 추가하여 캐싱합니다.
     * 캐시에 없을 경우 DB에서 조회 후 캐시에 저장합니다.
     * @return ShortUrl 조회된 ShortUrl 도메인 객체
     */
    fun findShortUrlByShortKey(shortKey: String): ShortUrl? {
        val cachedShortUrl = shortUrlRepository.findShortUrlByShortKeyInCache(shortKey)
        if (cachedShortUrl != null) {
            return cachedShortUrl
        }
        val shortUrl = shortUrlRepository.findByShortKey(shortKey)?.toDomain()
        if(shortUrl != null) {
            val ttl = calculateDynamicTtl()
            shortUrlRepository.cacheShortUrlByShortKey(shortUrl, ttl)
        }
        return shortUrl
    }

    /**
     * 캐시에서 originalUrl로 ShortUrl을 조회합니다.
     * Cacheable 어노테이션을 사용하여 자동으로 캐싱 처리합니다.
     * 캐시에 없을 경우 DB에서 조회 후 캐시에 저장합니다.
     * @return ShortUrl 조회된 ShortUrl 도메인 객체
     */
    @Cacheable(cacheNames = [CacheNames.SHORT_URL_BY_ORIGINAL], key = "#originalUrl")
    open fun findShortUrlByOriginalUrl(originalUrl: String): ShortUrl? {
        return shortUrlRepository.findByOriginalUrl(originalUrl)?.toDomain()
    }

    /**
     * 기본 TTL(1일) + Jitter(0~60분) 적용
     */
    private fun calculateDynamicTtl(): Duration {
        val ttlSeconds = 86400L // 기본 1일 (24시간)
        val jitterRangeSeconds = 3600L // Jitter 범위 1시간
        val jitter = ThreadLocalRandom.current().nextLong(jitterRangeSeconds)
        val standardTtlSeconds = ttlSeconds + jitter
        return Duration.ofSeconds(standardTtlSeconds)
    }
}
