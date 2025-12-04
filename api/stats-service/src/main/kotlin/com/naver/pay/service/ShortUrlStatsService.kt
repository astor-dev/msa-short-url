package com.naver.pay.service

import com.naver.pay.controller.v1.ShortUrlStateResponseDto
import com.naver.pay.shorturl.resolved.ResolvedShortUrlCachableService
import org.springframework.stereotype.Service

@Service
class ShortUrlStatsService(
    private val resolvedShortUrlCachableService: ResolvedShortUrlCachableService
) {
    fun findShortUrlState(shortKey: String): ShortUrlStateResponseDto? {
        return try {
            resolvedShortUrlCachableService.findResolvedShortUrlOrThrow(shortKey).toDto()
        } catch (_: NoSuchElementException) {
            null
        }
    }
}