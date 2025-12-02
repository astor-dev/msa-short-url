package com.naver.pay.shorturl

import com.naver.pay.shorturl.infrastructure.jpa.ShortUrlEntity
import com.naver.pay.shorturl.infrastructure.jpa.ShortUrlRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ShortUrlCachableService(
    private val shortUrlRepository: ShortUrlRepository
) {

    @Cacheable(cacheNames = ["shortUrlByShortKey"], key = "#shortKey")
    fun findShortUrlByShortKeyOrThrow(shortKey: String): ShortUrlEntity {
        return shortUrlRepository.findByShortKey(shortKey)
            .orElseThrow { NoSuchElementException("Short URL not found for short key: $shortKey") }
    }

    @Cacheable(cacheNames = ["shortUrlByOriginalUrl"], key = "#originalUrl")
    fun findShortUrlByOriginalUrlOrThrow(originalUrl: String): ShortUrlEntity {
        return shortUrlRepository.findByOriginalUrl(originalUrl)
            .orElseThrow { NoSuchElementException("Short URL not found for original URL: $originalUrl") }
    }
}
