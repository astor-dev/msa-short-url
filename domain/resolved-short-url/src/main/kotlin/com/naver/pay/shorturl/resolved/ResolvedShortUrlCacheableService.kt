package com.naver.pay.shorturl.resolved

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ResolvedShortUrlCacheableService(
    private val shortUrlResolvingService: ShortUrlResolvingService
) {
    /**
     * 캐시에서 shortKey로 ResolvedShortUrl을 조회합니다.
     * Cacheable 어노테이션을 사용하여 자동으로 캐싱 처리합니다.
     * 캐시에 없을 경우 resolving 로직을 수행합니다.
     * @throws NoSuchElementException 해당 shortKey에 대한 객체가 존재하지 않을 경우
     * @return ResolvedShortUrl 메타데이터를 포함한 단축 Url 정보
     */
    @Cacheable(value = [CacheNames.RESOLVED_SHORT_URL_BY_SHORT_KEY], key = "#shortKey")
    fun findResolvedShortUrlOrThrow(shortKey: String): ResolvedShortUrl {
        return shortUrlResolvingService.resolveShortUrl(shortKey)
            ?: throw NoSuchElementException("Short URL not found: $shortKey")
    }
}