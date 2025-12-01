package com.naver.pay.service

import com.naver.pay.domain.ShortUrl
import org.springframework.stereotype.Service


@Service
class ShortUrlService {
    final val BASE_URL = "https://short.naver.com/"

    fun create(originalUrl: String, ttlSeconds: Int): ShortUrl {
        val shortKey =  generateShortKey(originalUrl)
        val shortUrl = BASE_URL + shortKey
        return ShortUrl.of(
            shortKey = shortKey,
            shortUrl = shortUrl,
            originalUrl = originalUrl,
            ttlSeconds = ttlSeconds
        )
    }

    private fun generateShortKey(originalUrl: String): String {
        TODO()
    }
}