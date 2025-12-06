package com.naver.pay.shorturl

import com.naver.pay.shorturl.exception.ExpiredLinkException
import org.springframework.stereotype.Service

@Service
class RedirectUrlService(
    private val shortUrlRepository: ShortUrlRepository
) {

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