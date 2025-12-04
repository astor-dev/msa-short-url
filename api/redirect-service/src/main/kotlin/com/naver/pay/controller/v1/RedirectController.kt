package com.naver.pay.controller.v1

import com.naver.pay.service.RedirectService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/urls")
class RedirectController(
    private val redirectService: RedirectService
) {

    @GetMapping("/{shortKey}")
    fun getRedirectUrl(
        @PathVariable shortKey: String,
        @RequestHeader("User-Agent", required = false) userAgent: String?,
        @RequestHeader("Referer", required = false) referrer: String?
    ): ResponseEntity<RedirectUrlResponseDto> {
        return ResponseEntity.ok(
            redirectService.getRedirectUrl(
                shortKey= shortKey,
                userAgent = userAgent,
                referrer = referrer
            )
        )
    }
}