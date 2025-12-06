package com.naver.pay.controller

import com.naver.pay.converter.toDto
import com.naver.pay.shorturl.ShortUrlService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                shortUrlService.create(
                    originalUrl = requestDto.originalUrl,
                    ttlSeconds = requestDto.ttlSeconds
                ).toDto()
            )
    }
}