package com.naver.pay.shorturl

import java.time.Duration

interface ShortUrlCacheService {
    fun findShortUrlByShortKey(shortKey: String): ShortUrl?
    fun cacheShortUrlByShortKey(shortUrl: ShortUrl, ttl: Duration)
}