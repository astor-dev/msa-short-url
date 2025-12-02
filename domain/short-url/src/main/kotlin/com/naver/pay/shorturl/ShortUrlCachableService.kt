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
    fun findShortUrlByShortKeyOrThrow(shortKey: String): ShortUrl {
        return shortUrlRepository.findByShortKey(shortKey)
            .map { it.toDomain() }
            .orElseThrow { NoSuchElementException("Short URL not found for short key: $shortKey") }
    }

    @Cacheable(cacheNames = ["shortUrlByOriginalUrl"], key = "#originalUrl")
    fun findShortUrlByOriginalUrlOrThrow(originalUrl: String): ShortUrl {
        return shortUrlRepository.findByOriginalUrl(originalUrl)
            .map { it.toDomain() }
            .orElseThrow { NoSuchElementException("Short URL not found for original URL: $originalUrl") }
    }
}
