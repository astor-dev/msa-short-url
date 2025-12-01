package com.naver.pay.controller.v1

import com.naver.pay.service.ShortUrlService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/urls")
class UrlController(
    private val shortUrlService: ShortUrlService
) {
    @PostMapping
    fun createShortUrl(@Valid @RequestBody requestDto: UrlRequestDto): ResponseEntity<UrlResponseDto> {
        val shortUrl = shortUrlService.create(
            originalUrl = requestDto.originalUrl,
            ttlSeconds = requestDto.ttlSeconds
        )
        return ResponseEntity.ok(
            UrlResponseDto(
                shortKey = shortUrl.shortKey,
                shortUrl = shortUrl.shortUrl,
                originalUrl = shortUrl.originalUrl,
                createdAt = shortUrl.createdAt.toString(),
                expiresAt = shortUrl.expiresAt.toString()
            )
        )
    }
}