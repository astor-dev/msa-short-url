package com.naver.pay.shorturl.resolved

import com.naver.pay.shorturl.ShortUrlCachableService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ShortUrlResolvingService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val shortUrlSummaryService: ShortUrlSummaryService,
    private val shortUrlCachableService: ShortUrlCachableService
) {

    /**
     * 메타데이터가 포함된 ShortUrl을 조회합니다.
     * @param shortKey
     * @return ResolvedShortUrl shortKey에 해당하는 값이 존재하지 않는 경우 null을 return 합니다.
     */
    fun resolveShortUrl(shortKey: String): ResolvedShortUrl? {
        val shortUrl = try {
            shortUrlCachableService.findShortUrlByShortKeyOrThrow(shortKey)
        } catch (_: NoSuchElementException) {
            return null
        }

        val totalClicksCacheKey = "${CacheNames.SHORT_URL_TOTAL_CLICKS}::$shortKey"
        val lastClickedAtCacheKey = "${CacheNames.SHORT_URL_LAST_CLICKED_AT}::$shortKey"
        val clickSummary = findClickSummaryFromCache(totalClicksCacheKey, lastClickedAtCacheKey)
            ?: shortUrlSummaryService.findClickSummaryFromPersistence(shortKey)
        val resolvedShortUrl = ResolvedShortUrl(
            shortKey = shortKey,
            shortUrl = "${shortUrl.baseUrl}/$shortKey",
            originalUrl = shortUrl.originalUrl,
            createdAt =  shortUrl.createdAt,
            expiredAt = shortUrl.expiresAt,
            clickSummary = clickSummary,
        )
        return resolvedShortUrl
    }

    private fun findClickSummaryFromCache(totalClicksCacheKey: String, lastClickedAtCacheKey: String): ClickSummary? {
        if (!redisTemplate.hasKey(totalClicksCacheKey)) {
            return null
        }
        val opsForValue = redisTemplate.opsForValue()
        val totalClicks = opsForValue.get(totalClicksCacheKey)?.toLong()
        val lastClickedAt = opsForValue.get(lastClickedAtCacheKey)?.let { Instant.parse(it) }

        return if (totalClicks != null && lastClickedAt != null) {
            ClickSummary(totalClicks, lastClickedAt)
        } else {
            null
        }
    }
}