package com.naver.pay.shorturl

import com.naver.pay.shorturl.jpa.ShortUrlEntity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class ShortUrlCachableServiceTest : BehaviorSpec({

    val shortUrlRepositroy = mockk<ShortUrlRepository>()
    val shortUrlCacheableService = ShortUrlCacheableService(shortUrlRepositroy)

    afterTest {
        clearAllMocks()
    }

    Given("ShortUrlCacheableService가 주어졌을 때") {
        val shortKey = "testKey"
        val originalUrl = "https://naver.com"
        val baseUrl = "http://localhost"
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val expiresAt = now.plusSeconds(86400)
        val shortUrlDomain = ShortUrl.of(
            id = 1L,
            shortKey = shortKey,
            baseUrl = baseUrl,
            originalUrl = originalUrl,
            createdAt = now,
            expiresAt = expiresAt
        )
        val shortUrlEntity = ShortUrlEntity(
            id = 1L,
            shortKey = shortKey,
            baseUrl = baseUrl,
            originalUrl = originalUrl,
            expiresAt = expiresAt,
            createdAt = now,
            updatedAt = now
        )


        When("findShortUrlByShortKey 호출 시") {
            And("캐시에 ShortUrl이 존재하는 경우") {
                every { shortUrlRepositroy.findShortUrlByShortKeyInCache(shortKey) } returns shortUrlDomain

                val result = shortUrlCacheableService.findShortUrlByShortKey(shortKey)

                Then("캐시된 ShortUrl을 반환해야 한다") {
                    result?.id shouldBe shortUrlDomain.id
                    verify(exactly = 1) { shortUrlRepositroy.findShortUrlByShortKeyInCache(shortKey) }
                    verify(exactly = 0) { shortUrlRepositroy.findByShortKey(any()) }
                    verify(exactly = 0) { shortUrlRepositroy.cacheShortUrlByShortKey(any(), any()) }
                }
            }

            And("캐시에 ShortUrl이 존재하지 않고, DB에 존재하는 경우") {
                every { shortUrlRepositroy.findShortUrlByShortKeyInCache(shortKey) } returns null
                every { shortUrlRepositroy.findByShortKey(shortKey) } returns shortUrlEntity
                every { shortUrlRepositroy.cacheShortUrlByShortKey(any(), any()) } returns Unit

                val result = shortUrlCacheableService.findShortUrlByShortKey(shortKey)

                Then("DB에서 조회 후 캐시에 저장하고 ShortUrl을 반환해야 한다") {
                    result?.id shouldBe shortUrlDomain.id
                    verify(exactly = 1) { shortUrlRepositroy.findShortUrlByShortKeyInCache(shortKey) }
                    verify(exactly = 1) { shortUrlRepositroy.findByShortKey(shortKey) }
                    verify(exactly = 1) { shortUrlRepositroy.cacheShortUrlByShortKey(any(), any<Duration>()) }
                }
            }

            And("캐시에도 DB에도 ShortUrl이 존재하지 않는 경우") {
                every { shortUrlRepositroy.findShortUrlByShortKeyInCache(shortKey) } returns null
                every { shortUrlRepositroy.findByShortKey(shortKey) } returns null
                val result = shortUrlCacheableService.findShortUrlByShortKey(shortKey)

                Then("null을 반환 해야 한다") {
                    result shouldBe null
                    verify(exactly = 1) { shortUrlRepositroy.findShortUrlByShortKeyInCache(shortKey) }
                    verify(exactly = 1) { shortUrlRepositroy.findByShortKey(shortKey) }
                    verify(exactly = 0) { shortUrlRepositroy.cacheShortUrlByShortKey(any(), any()) }
                }
            }
        }

        When("findShortUrlByOriginalUrl 호출 시") {
            And("DB에 ShortUrl이 존재하는 경우") {
                every { shortUrlRepositroy.findByOriginalUrl(originalUrl) } returns shortUrlEntity

                val result = shortUrlCacheableService.findShortUrlByOriginalUrl(originalUrl)

                Then("DB에서 조회한 ShortUrl을 반환해야 한다") {
                    result?.id shouldBe shortUrlDomain.id
                    verify(exactly = 1) { shortUrlRepositroy.findByOriginalUrl(originalUrl) }
                }
            }

            And("DB에 ShortUrl이 존재하지 않는 경우") {
                every { shortUrlRepositroy.findByOriginalUrl(any()) } returns null
                val result = shortUrlCacheableService.findShortUrlByOriginalUrl(originalUrl)

                Then("null을 반환 해야 한다") {
                    result shouldBe null
                    verify(exactly = 1) { shortUrlRepositroy.findByOriginalUrl(originalUrl) }
                }
            }
        }
    }
})