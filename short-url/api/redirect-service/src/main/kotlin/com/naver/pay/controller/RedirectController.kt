package com.naver.pay.controller

import com.naver.pay.service.RedirectService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

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
    ): ResponseEntity<Void> {
        val redirectUrl = redirectService.getRedirectUrl(
            shortKey = shortKey,
            userAgent = userAgent,
            referrer = referrer
        ) ?: throw NoSuchElementException("shortUrl을 찾을 수 없습니다.")

        val headers = HttpHeaders()
        headers.location = URI.create(redirectUrl)

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .headers(headers)
            .build()
    }
}
