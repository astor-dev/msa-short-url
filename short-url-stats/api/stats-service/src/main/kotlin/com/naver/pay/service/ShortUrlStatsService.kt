package com.naver.pay.service

import com.naver.pay.controller.DailyTopStatsResponseDto
import com.naver.pay.controller.ShortUrlStateResponseDto
import com.naver.pay.controller.ShortUrlStatisticsResponseDto
import com.naver.pay.controller.toDto
import com.naver.pay.controller.toStateDto
import com.naver.pay.controller.toStatsDto
import com.naver.pay.shorturl.stats.DailyTopStatsService
import com.naver.pay.shorturl.stats.TotalStatsService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ShortUrlStatsService(
    private val totalStatsService: TotalStatsService,
    private val shortUrlDailyTopStatsService: DailyTopStatsService
) {
    fun findShortUrlStateOrThrow(shortKey: String): ShortUrlStateResponseDto {
        return totalStatsService.findOne(shortKey)?.toStateDto()
            ?: throw NoSuchElementException("Short URL not found: $shortKey")
    }

    fun findShortUrlTotalStatsOrThrow(shortKey: String): ShortUrlStatisticsResponseDto {
        return totalStatsService.findOne(shortKey)?.toStatsDto()
            ?: throw NoSuchElementException("Short URL not found: $shortKey")
    }

    fun findShortUrlDailyTopStatsOrThrow(date: LocalDate, limit: Long): DailyTopStatsResponseDto {
        return shortUrlDailyTopStatsService.getOne(date, limit).toDto()
    }
}