package com.naver.pay.shorturl

import com.naver.pay.util.DistributedLockExecutor
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

/**
 * ShortUrl 조회 시 캐싱을 처리하는 서비스입니다.
 * 
 * 분산 락을 사용하여 Thundering herd를 방지하고, Jitter를 적용하여 Cache Stampede를 방지합니다.
 */
@Service
open class ShortUrlCacheableService(
    private val shortUrlRepository: ShortUrlRepository,
    private val distributedLockExecutor: DistributedLockExecutor
) {

    /**
     * 캐시에서 shortKey로 ShortUrl을 조회합니다.
     * 
     * 캐시에 없을 경우 DB에서 조회 후 캐시에 저장합니다.
     * Cache Stampede 방지를 위해 Jitter(0~60분)를 추가하여 캐싱합니다.
     * Thundering herd 방지를 위해 조회 로직에 분산락을 사용합니다.
     * 
     * @param shortKey 조회할 ShortUrl의 shortKey
     * @return 조회된 ShortUrl 도메인 객체, 없으면 null
     */
    fun findShortUrlByShortKey(shortKey: String): ShortUrl? {
        val cachedShortUrl = shortUrlRepository.findShortUrlByShortKeyInCache(shortKey)
        if (cachedShortUrl != null) {
            return cachedShortUrl
        }
        return distributedLockExecutor.execute(lockName = CacheNames.SHORT_URL_GET_LOCK, key = shortKey) {
            // 1. 락 획득 후 캐시 재확인
            var resolved = shortUrlRepository.findShortUrlByShortKeyInCache(shortKey)
            // 2. 여전히 캐시 미스 → DB 조회 및 캐시 저장
            if (resolved == null) {
                val fromDb = shortUrlRepository.findByShortKey(shortKey)?.toDomain()
                if (fromDb != null) {
                    val ttl = calculateDynamicTtl()
                    shortUrlRepository.cacheShortUrlByShortKey(fromDb, ttl)
                }
                resolved = fromDb
            }
            resolved
        }
    }

    /**
     * 캐시에서 originalUrl로 ShortUrl을 조회합니다.
     * 
     * Cacheable 어노테이션을 사용하여 자동으로 캐싱 처리합니다.
     * 캐시에 없을 경우 DB에서 조회 후 캐시에 저장합니다.
     * 주된 조회 경로가 아니기에 DB 부하 방지 보다는 락 비용이 크다고 여겨 별도의 처리를 하지 않습니다.
     * 
     * @param originalUrl 조회할 ShortUrl의 originalUrl
     * @return 조회된 ShortUrl 도메인 객체, 없으면 null
     */
    @Cacheable(cacheNames = [CacheNames.SHORT_URL_BY_ORIGINAL], key = "#originalUrl")
    open fun findShortUrlByOriginalUrl(originalUrl: String): ShortUrl? {
        return shortUrlRepository.findByOriginalUrl(originalUrl)?.toDomain()
    }

    /**
     * 기본 TTL(1일) + Jitter(0~60분)을 적용한 Duration을 계산합니다.
     * 
     * @return 계산된 TTL Duration
     */
    private fun calculateDynamicTtl(): Duration {
        val ttlSeconds = 86400L // 기본 1일 (24시간)
        val jitterRangeSeconds = 3600L // Jitter 범위 1시간
        val jitter = ThreadLocalRandom.current().nextLong(jitterRangeSeconds)
        val standardTtlSeconds = ttlSeconds + jitter
        return Duration.ofSeconds(standardTtlSeconds)
    }
}
