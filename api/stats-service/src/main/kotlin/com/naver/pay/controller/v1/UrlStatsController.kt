package com.naver.pay.controller.v1

import com.naver.pay.service.ShortUrlStatsService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("v1")
class UrlStatsController(
    private val shortUrlStatsService: ShortUrlStatsService
) {

    @GetMapping("/urls/{shortKey}")
    fun getShortUrlState(
        @PathVariable shortKey: String,
    ): ResponseEntity<ShortUrlStateResponseDto> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                shortUrlStatsService.findShortUrlStateOrThrow(shortKey)
            )
    }

    @GetMapping("/urls/{shortKey}/statistics")
    fun getShortUrlStats(
        @PathVariable shortKey: String,
    ): ResponseEntity<ShortUrlStatisticsResponseDto> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                shortUrlStatsService.findShortUrlTotalStatsOrThrow(shortKey)
            )
    }

    @GetMapping("/statistics/top")
    fun getDailyTopStats(
        @Valid @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @Valid @Max(100L) @RequestParam(defaultValue = "10") limit: Long,
    ): ResponseEntity<DailyTopStatsResponseDto> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                shortUrlStatsService.findShortUrlDailyTopStatsOrThrow(date, limit)
            )
    }
}