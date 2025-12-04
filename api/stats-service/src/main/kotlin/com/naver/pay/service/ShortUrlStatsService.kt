package com.naver.pay.service

import com.naver.pay.controller.v1.DailyTopStatsResponseDto
import com.naver.pay.controller.v1.ShortUrlStateResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsResponseDto
import com.naver.pay.shorturl.resolved.ResolvedShortUrlCachableService
import com.naver.pay.shorturl.stats.ShortUrlDailyTopStatsService
import com.naver.pay.shorturl.stats.ShortUrlTotalStatsService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ShortUrlStatsService(
    private val resolvedShortUrlCachableService: ResolvedShortUrlCachableService,
    private val shortUrlTotalStatsService: ShortUrlTotalStatsService,
    private val shortUrlDailyTopStatsService: ShortUrlDailyTopStatsService
) {
    fun findShortUrlStateOrThrow(shortKey: String): ShortUrlStateResponseDto {
        return resolvedShortUrlCachableService.findResolvedShortUrlOrThrow(shortKey).toDto()
    }

    fun findShortUrlTotalStatsOrThrow(shortKey: String): ShortUrlStatisticsResponseDto {
        return shortUrlTotalStatsService.findOne(shortKey)?.toDto()
            ?: throw NoSuchElementException("Short URL not found: $shortKey")
    }

    fun findShortUrlDailyTopStatsOrThrow(date: LocalDate, limit: Long): DailyTopStatsResponseDto {
        return shortUrlDailyTopStatsService.getOne(date, limit).toDto()
    }
}