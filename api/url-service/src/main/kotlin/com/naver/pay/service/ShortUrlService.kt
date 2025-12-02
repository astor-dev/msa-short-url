package com.naver.pay.service

import com.naver.pay.shorturl.ShortUrl
import org.springframework.stereotype.Service


@Service
class ShortUrlService {
    final val BASE_URL = "https://short.naver.com"

    fun create(originalUrl: String, ttlSeconds: Int): ShortUrl {
        val shortKey =  generateShortKey(originalUrl)
        return ShortUrl.of(
            shortKey = shortKey,
            baseUrl = BASE_URL,
            originalUrl = originalUrl,
            ttlSeconds = ttlSeconds
        )
    }

    private fun generateShortKey(originalUrl: String): String {
        TODO()
    }
}