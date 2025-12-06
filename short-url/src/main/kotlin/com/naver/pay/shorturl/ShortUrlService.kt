package com.naver.pay.shorturl

import com.naver.pay.shorturl.exception.ExpiredLinkException
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

    /**
     * 주어진 shortKey에 해당하는 RedirectUrl 도메인 객체를 반환합니다.
     *
     * @param shortKey 조회할 ShortUrl의 shortKey
     * @param userAgent 사용자 에이전트 (이벤트 발행용)
     * @param referrer 리퍼러 (이벤트 발행용)
     * @throws ExpiredLinkException 해당 링크가 만료된 경우
     * @return RedirectUrl 문자열, 없으면 null
     */
    fun getRedirectUrl(shortKey: String, userAgent: String?, referrer: String?): String? {
        val redirectUrl = shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
            ?: return null
        
        if (redirectUrl.expiresAt <= java.time.Instant.now()) {
            throw ExpiredLinkException(redirectUrl.url)
        }
        
        return redirectUrl.url
    }
}