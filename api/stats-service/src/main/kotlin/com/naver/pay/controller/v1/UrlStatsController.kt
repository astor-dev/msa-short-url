package com.naver.pay.controller.v1

import com.naver.pay.service.ShortUrlStatsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/urls")
class UrlStatsController(
    private val shortUrlStatsService: ShortUrlStatsService
) {

    @GetMapping("/{shortKey}")
    fun getShortUrlState(
        @PathVariable shortKey: String,
    ): ResponseEntity<ShortUrlStateResponseDto> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                shortUrlStatsService.findShortUrlStateOrThrow(shortKey)
            )
    }

    @GetMapping("/{shortKey}/statistics")
    fun getShortUrlStats(
        @PathVariable shortKey: String,
    ): ResponseEntity<ShortUrlStatisticsResponseDto> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                shortUrlStatsService.findShortUrlTotalStatsOrThrow(shortKey)
            )
    }
}