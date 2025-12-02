package com.naver.pay.service

import com.naver.pay.controller.v1.RedirectUrlResponseDto
import com.naver.pay.exception.ExpiredLinkException
import com.naver.pay.shorturl.ShortUrlCachableService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant


@Service
class RedirectService(
    private val shortUrlCachableService: ShortUrlCachableService
) {

    /**
     * 주어진 shortKey에 해당하는 원본 URL을 반환합니다.
     * 조회 후 클릭 이벤트를 아웃박스에 저장합니다.
     * @throws NoSuchElementException 해당 shortKey가 존재하지 않을 경우
     * @throws ExpiredLinkException 해당 링크가 만료된 경우
     * @return RedirectUrlResponseDto 원본 URL을 담은 DTO
     */
    @Transactional
    fun getRedirectUrl(shortKey: String): RedirectUrlResponseDto {
        val shortUrl = shortUrlCachableService.findShortUrlByShortKeyOrThrow(shortKey)
        println(Instant.now())
        println(shortUrl.expiresAt)
        println(shortUrl.expiresAt.isAfter(Instant.now()))
        if (shortUrl.expiresAt <= Instant.now()) {
            throw ExpiredLinkException(shortUrl.originalUrl)
        }
        // TODO: store event to outbox
        return RedirectUrlResponseDto(
            originalUrl = shortUrl.originalUrl
        )
    }
}