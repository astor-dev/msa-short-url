package com.naver.pay.shorturl

import com.naver.pay.outbox.OutboxService
import com.naver.pay.shorturl.infrastructure.jpa.ShortUrlEntity
import com.naver.pay.shorturl.infrastructure.jpa.ShortUrlRepository
import com.naver.pay.shorturl.infrastructure.stream.Bindings
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class ShortUrlStoreService(
    private val shortUrlRepository: ShortUrlRepository,
    private val outboxService: OutboxService
){

    /**
     * 짧은 URL을 생성하고 저장합니다.
     * outbox에 SHORT_URL_CREATED 이벤트를 발행합니다.
     * @param baseUrl 짧은 URL의 기본 URL
     * @param originalUrl 원본 URL
     * @param ttlSeconds URL의 유효 기간(초)
     * @return 생성된 ShortUrl 객체
     */
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
        val updatedShortUrl = shortUrlRepository.save(ShortUrlEntity.of(shortUrlWithKey)).toDomain()

        // NOTE: 논리 상 shortKey는 null이 될 수 없으나, 안전성을 위해 null 체크를 수행합니다.
        updatedShortUrl.shortKey?.let {
            outboxService.storeEvent(Bindings.SHORT_URL_CREATED, ShortUrlCreatedPayload(
                shortKey = it,
                originalUrl = updatedShortUrl.originalUrl,
                shortUrl = updatedShortUrl.generateShortUrlOrThrow(),
                shortUrlCreatedAt = updatedShortUrl.createdAt,
                shortUrlExpiredAt = updatedShortUrl.expiresAt
            ))
        } ?: throw IllegalStateException("Short key should not be null after generation.")
        return updatedShortUrl
    }
}