package com.naver.pay.service

import com.naver.pay.exception.ExpiredLinkException
import com.naver.pay.shorturl.ShortUrl
import com.naver.pay.shorturl.ShortUrlCacheableService
import com.naver.pay.shorturl.ShortUrlClickedPayload
import com.naver.pay.shorturl.infrastructure.stream.ShortUrlEventProducer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.NoSuchElementException

class RedirectServiceTest : BehaviorSpec({

    val shortUrlCacheableService = mockk<ShortUrlCacheableService>()
    val shortUrlEventProducer = mockk<ShortUrlEventProducer>()

    val redirectService = RedirectService(shortUrlCacheableService, shortUrlEventProducer)

    beforeEach {
        clearMocks(
            shortUrlCacheableService,
            shortUrlEventProducer,
            answers = false,
            recordedCalls = true
        )
    }

    Given("RedirectService의 getRedirectUrl 메서드가 호출될 때") {
        val shortKey = "testKey"
        val originalUrl = "https://example.com/long/original/url"
        val userAgent = "test-agent"
        val referrer = "test-referrer"
        val now = Instant.now()
        When("유효하고 만료되지 않은 shortKey가 주어지면") {
            val unexpiredShortUrl = ShortUrl.of(
                id = 1L,
                shortKey = shortKey,
                baseUrl = "http://localhost",
                originalUrl = originalUrl,
                createdAt = now.minusSeconds(60 * 30),
                expiresAt = now.plusSeconds(60 * 60)
            )

            every { shortUrlCacheableService.findShortUrlByShortKeyOrThrow(shortKey) } returns unexpiredShortUrl
            every { shortUrlEventProducer.publishUrlClicked(any(), any()) } returns Unit


            Then("원본 URL을 반환해야 한다") {
                val result = redirectService.getRedirectUrlOrThrow(shortKey, userAgent, referrer)
                result shouldBe originalUrl
            }

            Then("클릭 이벤트를 발행해야 한다") {
                redirectService.getRedirectUrlOrThrow(shortKey, userAgent, referrer)
                verify(exactly = 1) {
                    shortUrlEventProducer.publishUrlClicked(
                        shortKey,
                        match<ShortUrlClickedPayload> {
                            it.shortKey == shortKey && it.userAgent == userAgent && it.referrer == referrer
                        }
                    )
                }
            }
            
            Then("user agent와 referrer가 null일 경우 기본값으로 클릭 이벤트를 발행해야 한다") {
                redirectService.getRedirectUrlOrThrow(shortKey, null, null)
                verify(exactly = 1) {
                    shortUrlEventProducer.publishUrlClicked(
                        shortKey,
                        match<ShortUrlClickedPayload> {
                            it.shortKey == shortKey && it.userAgent == "Unknown" && it.referrer == "Direct"
                        }
                    )
                }
            }
        }

        When("만료된 shortKey가 주어지면") {
            val expiredShortUrl = ShortUrl.of(
                id = 1L,
                shortKey = shortKey,
                baseUrl = "http://localhost",
                originalUrl = originalUrl,
                createdAt = now.minusSeconds(60 * 60 * 2),
                expiresAt = now.minusSeconds(60 * 60)
            )
            every { shortUrlCacheableService.findShortUrlByShortKeyOrThrow(shortKey) } returns expiredShortUrl

            Then("ExpiredLinkException을 던져야 한다") {
                shouldThrow<ExpiredLinkException> {
                    redirectService.getRedirectUrlOrThrow(shortKey, userAgent, referrer)
                }
            }

            Then("클릭 이벤트를 발행하지 않아야 한다") {
                shouldThrow<ExpiredLinkException> {
                    redirectService.getRedirectUrlOrThrow(shortKey, userAgent, referrer)
                }
                verify(exactly = 0) { shortUrlEventProducer.publishUrlClicked(any(), any()) }
            }
        }

        When("존재하지 않는 shortKey가 주어지면") {
            every { shortUrlCacheableService.findShortUrlByShortKeyOrThrow(any()) } throws NoSuchElementException("Short URL not found")

            Then("NoSuchElementException을 던져야 한다") {
                shouldThrow<NoSuchElementException> {
                    redirectService.getRedirectUrlOrThrow("nonExistentKey", userAgent, referrer)
                }
            }

            Then("클릭 이벤트를 발행하지 않아야 한다") {
                shouldThrow<NoSuchElementException> {
                    redirectService.getRedirectUrlOrThrow("nonExistentKey", userAgent, referrer)
                }
                verify(exactly = 0) { shortUrlEventProducer.publishUrlClicked(any(), any()) }
            }
        }
    }
})
