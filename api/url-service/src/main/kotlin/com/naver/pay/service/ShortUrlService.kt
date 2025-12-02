package com.naver.pay.service

import com.naver.pay.controller.v1.UrlResponseDto
import org.springframework.stereotype.Service


@Service
class ShortUrlService {
    final val BASE_URL = "https://short.naver.com"

    fun create(originalUrl: String, ttlSeconds: Int): UrlResponseDto {
        TODO()
    }
}