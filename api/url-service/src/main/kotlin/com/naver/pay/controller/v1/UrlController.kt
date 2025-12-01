package com.naver.pay.controller.v1

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/urls")
class UrlController {
    @PostMapping
    fun createShortUrl(@Valid @RequestBody requestDto: UrlRequestDto): ResponseEntity<UrlResponseDto> {
        return ResponseEntity.ok(
            UrlResponseDto(
                shortKey = "abc123",
                shortUrl = "http://short.url/abc123",
                originalUrl = requestDto.originalUrl,
                createdAt = "2024-01-01T00:00:00Z",
                expiresAt = "2024-01-02T00:00:00Z"
            )
        )
    }
}