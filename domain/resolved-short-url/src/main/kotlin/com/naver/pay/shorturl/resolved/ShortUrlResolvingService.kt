package com.naver.pay.shorturl.resolved

import com.naver.pay.shorturl.ShortUrlCacheableService
import org.springframework.stereotype.Service

@Service
class ShortUrlResolvingService(
    private val shortUrlSummaryService: ShortUrlSummaryService,
    private val resolvedShortUrlCacheService: ResolvedShortUrlCacheService,
    private val shortUrlCacheableService: ShortUrlCacheableService
) {

    /**
     * 메타데이터가 포함된 ShortUrl을 조회합니다.
     * @param shortKey
     * @return ResolvedShortUrl shortKey에 해당하는 값이 존재하지 않는 경우 null을 return 합니다.
     */
    fun resolveShortUrl(shortKey: String): ResolvedShortUrl? {
        val shortUrl = shortUrlCacheableService.findShortUrlByShortKey(shortKey)
            ?: return null
        val totalClicksCacheKey = "${CacheNames.SHORT_URL_TOTAL_CLICKS}::$shortKey"
        val lastClickedAtCacheKey = "${CacheNames.SHORT_URL_LAST_CLICKED_AT}::$shortKey"
        val clickSummary = resolvedShortUrlCacheService.findClickSummary(totalClicksCacheKey, lastClickedAtCacheKey)
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


}