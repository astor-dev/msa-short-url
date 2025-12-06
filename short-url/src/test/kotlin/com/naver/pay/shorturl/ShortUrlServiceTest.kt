package com.naver.pay.shorturl

import com.naver.pay.util.DistributedLockExecutor
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class ShortUrlServiceTest : BehaviorSpec({
    val shortUrlRepository = mockk<ShortUrlRepository>()
    val distributedLockExecutor = mockk<DistributedLockExecutor>()
    val shortUrlService = ShortUrlService(
        shortUrlRepository = shortUrlRepository,
        distributedLockExecutor = distributedLockExecutor
    )

    afterTest {
        clearAllMocks()
    }

    Given("create 메소드가 주어졌을 때") {
        val originalUrl = "https://naver.com"
        val ttlSeconds = 3600
        val now = Instant.now()
        val expiresAt = now.plusSeconds(ttlSeconds.toLong())
        val baseUrl = ShortUrlService.BASE_URL

        When("기존 ShortUrl이 존재하지 않는 경우") {
            val createdShortUrl = ShortUrl.of(
                id = 1L,
                shortKey = "testKey",
                baseUrl = baseUrl,
                originalUrl = originalUrl,
                createdAt = now,
                expiresAt = expiresAt
            )

            every {
                distributedLockExecutor.execute<ShortUrl>(
                    lockName = CacheNames.SHORT_URL_CREATE_LOCK,
                    key = originalUrl,
                    block = any()
                )
            } answers {
                val block = lastArg<() -> ShortUrl>()
                block.invoke()
            }
            every { shortUrlRepository.findShortUrlByOriginalUrl(originalUrl) } returns null
            every {
                shortUrlRepository.createShortUrl(
                    baseUrl = baseUrl,
                    originalUrl = originalUrl,
                    ttlSeconds = ttlSeconds
                )
            } returns createdShortUrl

            val result = shortUrlService.create(originalUrl, ttlSeconds)

            Then("새로운 ShortUrl을 생성하고 반환해야 한다") {
                result shouldBe createdShortUrl
                verify(exactly = 1) {
                    distributedLockExecutor.execute<ShortUrl>(
                        lockName = CacheNames.SHORT_URL_CREATE_LOCK,
                        key = originalUrl,
                        block = any()
                    )
                }
                verify(exactly = 1) { shortUrlRepository.findShortUrlByOriginalUrl(originalUrl) }
                verify(exactly = 1) {
                    shortUrlRepository.createShortUrl(
                        baseUrl = baseUrl,
                        originalUrl = originalUrl,
                        ttlSeconds = ttlSeconds
                    )
                }
            }
        }

        When("기존 ShortUrl이 존재하는 경우") {
            val existingShortUrl = ShortUrl.of(
                id = 1L,
                shortKey = "existingKey",
                baseUrl = baseUrl,
                originalUrl = originalUrl,
                createdAt = now.minusSeconds(7200),
                expiresAt = now.plusSeconds(1800)
            )

            every {
                distributedLockExecutor.execute<ShortUrl>(
                    lockName = CacheNames.SHORT_URL_CREATE_LOCK,
                    key = originalUrl,
                    block = any()
                )
            } answers {
                val block = lastArg<() -> ShortUrl>()
                block.invoke()
            }
            every { shortUrlRepository.findShortUrlByOriginalUrl(originalUrl) } returns existingShortUrl

            val result = shortUrlService.create(originalUrl, ttlSeconds)

            Then("기존 ShortUrl을 반환하고 새로 생성하지 않아야 한다") {
                result shouldBe existingShortUrl
                verify(exactly = 1) {
                    distributedLockExecutor.execute<ShortUrl>(
                        lockName = CacheNames.SHORT_URL_CREATE_LOCK,
                        key = originalUrl,
                        block = any()
                    )
                }
                verify(exactly = 1) { shortUrlRepository.findShortUrlByOriginalUrl(originalUrl) }
                verify(exactly = 0) {
                    shortUrlRepository.createShortUrl(
                        baseUrl = any(),
                        originalUrl = any(),
                        ttlSeconds = any()
                    )
                }
            }
        }
    }
})

