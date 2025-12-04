package com.naver.pay.shorturl.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.shorturl.CacheNames
import com.naver.pay.shorturl.ShortUrl
import com.naver.pay.shorturl.ShortUrlCacheService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.lang.IllegalStateException
import java.time.Duration

@Service
class ShortUrlRedisCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
): ShortUrlCacheService {
    override fun findShortUrlByShortKey(shortKey: String): ShortUrl? {
        val cacheKey = "${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey"
        val cachedValue = redisTemplate.opsForValue().get(cacheKey)
        val cachedShortUrl = runCatching {
            objectMapper.readValue(cachedValue, ShortUrl::class.java)
        }.getOrNull()
        return cachedShortUrl
    }

    override fun cacheShortUrlByShortKey(shortUrl: ShortUrl, ttl: Duration) {
        shortUrl.shortKey?.let {
            val cacheKey = "${CacheNames.SHORT_URL_BY_SHORT_KEY}::$it"
            val jsonString = objectMapper.writeValueAsString(shortUrl)
            redisTemplate.opsForValue().set(cacheKey, jsonString, ttl)
        } ?: throw IllegalStateException("Short Ket is not set: ${shortUrl.id}")
    }
}