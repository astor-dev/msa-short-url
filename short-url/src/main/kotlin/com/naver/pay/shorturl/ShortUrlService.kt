package com.naver.pay.shorturl

import com.naver.pay.util.DistributedLockExecutor
import org.springframework.stereotype.Service

@Service
class ShortUrlService(
    private val shortUrlRepository: ShortUrlRepository,
    private val distributedLockExecutor: DistributedLockExecutor
) {
    companion object {
        const val BASE_URL = "https://short.naver.com"
    }


    /**
     * originalUrl에 해당하는 shortUrl을 생성합니다.
     * 동일 params에 대해 멱등성을 보장합니다.
     * originalUrl 존재 검증 성공 이후 생성 처리 도중에,
     * 같은 originalUrl을 source로 shortUrl이 생성되어 멱등성이 깨지는 것을 막기위해 분산락을 쥡니다.
     * @param originalUrl shortUrl로 변환할 originalUrl
     * @param ttlSeconds 만료 시간
     * @return UrlResponseDto
     */
    fun create(originalUrl: String, ttlSeconds: Int): ShortUrl {
        return distributedLockExecutor.execute(
            lockName = CacheNames.SHORT_URL_CREATE_LOCK,
            key = originalUrl
        ) {
            val existing = shortUrlRepository.findShortUrlByOriginalUrl(originalUrl)
            if(existing == null) {
                val shortUrl = shortUrlRepository.createShortUrl(
                    baseUrl = BASE_URL,
                    originalUrl = originalUrl,
                    ttlSeconds = ttlSeconds
                )
                shortUrl
            } else {
                existing
            }
        }
    }
}