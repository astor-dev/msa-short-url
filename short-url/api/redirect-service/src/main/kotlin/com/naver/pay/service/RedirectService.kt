package com.naver.pay.service

import com.naver.pay.exception.ExpiredLinkException
import com.naver.pay.shorturl.ShortUrlCacheableService
import com.naver.pay.shorturl.stream.ShortUrlClickedPayload
import com.naver.pay.shorturl.stream.ShortUrlEventProducer
import org.springframework.stereotype.Service
import java.time.Instant


@Service
class RedirectService(
    private val shortUrlCacheableService: ShortUrlCacheableService,
    private val shortUrlEventProducer: ShortUrlEventProducer
) {

    /**
     * 주어진 shortKey에 해당하는 원본 URL String을 반환합니다.
     * 조회 후 클릭 이벤트를 발행합니다.
     * @throws ExpiredLinkException 해당 링크가 만료된 경우
     * @return String 원본 URL
     */
    fun getRedirectUrl(shortKey: String, userAgent: String?, referrer: String?): String? {
        val redirectUrl = shortUrlCacheableService.findShortUrlByShortKey(shortKey)
            ?.let {
                if (it.expiresAt <= Instant.now()) {
                    throw ExpiredLinkException(it.originalUrl)
                }
                shortUrlEventProducer.publishUrlClicked(
                    shortKey,
                    ShortUrlClickedPayload(
                        shortKey = shortKey,
                        userAgent = userAgent ?: "Unknown",
                        referrer = referrer ?: "Direct",
                        clickedAt = Instant.now()
                    )
                )
                it.originalUrl
            }
        return redirectUrl
    }
}
