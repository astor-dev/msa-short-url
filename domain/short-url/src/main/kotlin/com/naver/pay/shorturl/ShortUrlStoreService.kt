package com.naver.pay.shorturl

import com.naver.pay.shorturl.infrastructure.jpa.ShortUrlEntity
import com.naver.pay.shorturl.infrastructure.jpa.ShortUrlRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class ShortUrlStoreService(
    private val shortUrlRepository: ShortUrlRepository,
){
    @Transactional
    fun createShortUrl(
        baseUrl: String,
        originalUrl: String,
        ttlSeconds: Int
    ): ShortUrl {
        val noKeyShortUrl = ShortUrl.generate(
            shortKey = null,
            baseUrl = baseUrl,
            originalUrl = originalUrl,
            ttlSeconds = ttlSeconds
        )
        val savedShortUrl = shortUrlRepository.save(ShortUrlEntity.of(noKeyShortUrl)).toDomain()
        val shortUrlWithKey = savedShortUrl.generateShortKeyFromId()
        val updatedShortUrlEntity = shortUrlRepository.save(ShortUrlEntity.of(shortUrlWithKey)).toDomain()
        return updatedShortUrlEntity
    }
}