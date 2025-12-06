package com.naver.pay.shorturl

import com.naver.pay.shorturl.exception.ExpiredLinkException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class RedirectUrlServiceTest : BehaviorSpec({
    val shortUrlRepository = mockk<ShortUrlRepository>()
    val redirectUrlService = RedirectUrlService(
        shortUrlRepository = shortUrlRepository
    )

    afterTest {
        clearAllMocks()
    }

    Given("getRedirectUrl 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val originalUrl = "https://naver.com"
        val userAgent = "test-agent"
        val referrer = "test-referrer"
        val now = Instant.now()

        When("RedirectUrl이 존재하고 만료되지 않은 경우") {
            val expiresAt = now.plusSeconds(3600)
            val redirectUrl = RedirectUrl(url = originalUrl, expiresAt = expiresAt)

            every {
                shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
            } returns redirectUrl

            val result = redirectUrlService.getRedirectUrl(shortKey, userAgent, referrer)

            Then("원본 URL을 반환해야 한다") {
                result shouldBe originalUrl
                verify(exactly = 1) {
                    shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
                }
            }
        }

        When("RedirectUrl이 존재하지만 만료된 경우") {
            val expiresAt = now.minusSeconds(3600)
            val redirectUrl = RedirectUrl(url = originalUrl, expiresAt = expiresAt)

            every {
                shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
            } returns redirectUrl

            Then("ExpiredLinkException을 던져야 한다") {
                shouldThrow<ExpiredLinkException> {
                    redirectUrlService.getRedirectUrl(shortKey, userAgent, referrer)
                }
                verify(exactly = 1) {
                    shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
                }
            }
        }

        When("RedirectUrl이 만료 시점과 현재 시점이 동일한 경우") {
            val expiresAt = now
            val redirectUrl = RedirectUrl(url = originalUrl, expiresAt = expiresAt)

            every {
                shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
            } returns redirectUrl

            Then("ExpiredLinkException을 던져야 한다") {
                shouldThrow<ExpiredLinkException> {
                    redirectUrlService.getRedirectUrl(shortKey, userAgent, referrer)
                }
                verify(exactly = 1) {
                    shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
                }
            }
        }

        When("RedirectUrl이 존재하지 않는 경우") {
            every {
                shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
            } returns null

            val result = redirectUrlService.getRedirectUrl(shortKey, userAgent, referrer)

            Then("null을 반환해야 한다") {
                result shouldBe null
                verify(exactly = 1) {
                    shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
                }
            }
        }

        When("userAgent와 referrer가 null인 경우") {
            val expiresAt = now.plusSeconds(3600)
            val redirectUrl = RedirectUrl(url = originalUrl, expiresAt = expiresAt)

            every {
                shortUrlRepository.getRedirectUrl(shortKey, null, null)
            } returns redirectUrl

            val result = redirectUrlService.getRedirectUrl(shortKey, null, null)

            Then("정상적으로 원본 URL을 반환해야 한다") {
                result shouldBe originalUrl
                verify(exactly = 1) {
                    shortUrlRepository.getRedirectUrl(shortKey, null, null)
                }
            }
        }
    }
})

