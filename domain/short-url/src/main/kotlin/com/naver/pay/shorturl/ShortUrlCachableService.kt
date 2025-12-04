package com.naver.pay.shorturl

import com.naver.pay.shorturl.infrastructure.jpa.ShortUrlRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

@Service
class ShortUrlCachableService(
    private val shortUrlRepository: ShortUrlRepository,
    private val shortUrlCacheService: ShortUrlCacheService,
) {

    /**
     * 캐시에서 shortKey로 ShortUrl을 조회합니다.
     * 24시간 기본 TTL에 Jitter(0~60분)를 추가하여 캐싱합니다.
     * 캐시에 없을 경우 DB에서 조회 후 캐시에 저장합니다.
     * @throws NoSuchElementException 해당 shortKey가 존재하지 않을 경우
     * @return ShortUrl 조회된 ShortUrl 도메인 객체
     */
    fun findShortUrlByShortKeyOrThrow(shortKey: String): ShortUrl {
        val cachedShortUrl = shortUrlCacheService.findShortUrlByShortKey(shortKey)
        if (cachedShortUrl != null) {
            return cachedShortUrl
        }
        val shortUrl = shortUrlRepository.findByShortKey(shortKey)
            .map { it.toDomain() }
            .orElseThrow { NoSuchElementException("Short URL not found: $shortKey") }
        val ttl = calculateDynamicTtl()
        shortUrlCacheService.cacheShortUrlByShortKey(shortUrl, ttl)
        return shortUrl
    }

    /**
     * 캐시에서 originalUrl로 ShortUrl을 조회합니다.
     * Cacheable 어노테이션을 사용하여 자동으로 캐싱 처리합니다.
     * 캐시에 없을 경우 DB에서 조회 후 캐시에 저장합니다.
     * @throws NoSuchElementException 해당 originalUrl이 존재하지 않을 경우
     * @return ShortUrl 조회된 ShortUrl 도메인 객체
     */
    @Cacheable(cacheNames = [CacheNames.SHORT_URL_BY_ORIGINAL], key = "#originalUrl")
    fun findShortUrlByOriginalUrlOrThrow(originalUrl: String): ShortUrl {
        return shortUrlRepository.findByOriginalUrl(originalUrl)
            .map { it.toDomain() }
            .orElseThrow { NoSuchElementException("Short URL not found for original URL: $originalUrl") }
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
