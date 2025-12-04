package com.naver.pay.service

import com.naver.pay.controller.v1.ShortUrlStateResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsResponseDto
import com.naver.pay.shorturl.resolved.ResolvedShortUrlCachableService
import com.naver.pay.shorturl.stats.ShortUrlTotalStatsService
import com.naver.pay.shorturl.stats.infrastructure.mongodb.ShortUrlTotalStatsRepository
import org.springframework.stereotype.Service

@Service
class ShortUrlStatsService(
    private val resolvedShortUrlCachableService: ResolvedShortUrlCachableService,
    private val shortUrlTotalStatsService: ShortUrlTotalStatsService,
) {
    fun findShortUrlStateOrThrow(shortKey: String): ShortUrlStateResponseDto {
        return resolvedShortUrlCachableService.findResolvedShortUrlOrThrow(shortKey).toDto()
    }

    fun findShortUrlTotalStatsOrThrow(shortKey: String): ShortUrlStatisticsResponseDto {
        return shortUrlTotalStatsService.findOne(shortKey)?.toDto()
            ?: throw NoSuchElementException("Short URL not found: $shortKey")
    }
}