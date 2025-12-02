package com.naver.pay.service

import com.naver.pay.controller.v1.UrlResponseDto
import com.naver.pay.shorturl.ShortUrlCachableService
import com.naver.pay.shorturl.ShortUrlStoreService
import org.springframework.stereotype.Service


@Service
class ShortUrlService(
    private val shortUrlStoreService: ShortUrlStoreService,
    private val shortUrlCachableService: ShortUrlCachableService
) {

    final val BASE_URL = "https://short.naver.com"

    fun create(originalUrl: String, ttlSeconds: Int): UrlResponseDto {
        return try {
            shortUrlCachableService.findShortUrlByOriginalUrlOrThrow(originalUrl).toDto()
        } catch (_: NoSuchElementException) {
            val shortUrl = shortUrlStoreService.createShortUrl(
                baseUrl = BASE_URL,
                originalUrl = originalUrl,
                ttlSeconds = ttlSeconds
            )
            shortUrl.toDto()
        }
    }
}