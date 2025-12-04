package com.naver.pay.service

import com.naver.pay.controller.v1.RedirectUrlResponseDto
import com.naver.pay.exception.ExpiredLinkException
import com.naver.pay.shorturl.ShortUrlCachableService
import com.naver.pay.shorturl.ShortUrlClickedPayload
import com.naver.pay.shorturl.infrastructure.stream.ShortUrlEventProducer
import org.springframework.stereotype.Service
import java.time.Instant


@Service
class RedirectService(
    private val shortUrlCachableService: ShortUrlCachableService,
    private val shortUrlEventProducer: ShortUrlEventProducer
) {

    /**
     * 주어진 shortKey에 해당하는 원본 URL을 반환합니다.
     * 조회 후 클릭 이벤트를 발행합니다.
     * @throws NoSuchElementException 해당 shortKey가 존재하지 않을 경우
     * @throws ExpiredLinkException 해당 링크가 만료된 경우
     * @return RedirectUrlResponseDto 원본 URL을 담은 DTO
     */
    fun getRedirectUrl(shortKey: String, userAgent: String?, referrer: String?): RedirectUrlResponseDto {
        val shortUrl = shortUrlCachableService.findShortUrlByShortKeyOrThrow(shortKey)
        if (shortUrl.expiresAt <= Instant.now()) {
            throw ExpiredLinkException(shortUrl.originalUrl)
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
        return RedirectUrlResponseDto(
            originalUrl = shortUrl.originalUrl
        )
    }
}