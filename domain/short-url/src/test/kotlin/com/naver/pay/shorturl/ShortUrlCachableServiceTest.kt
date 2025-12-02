package com.naver.pay.shorturl

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.shorturl.infrastructure.jpa.ShortUrlEntity
import com.naver.pay.shorturl.infrastructure.jpa.ShortUrlRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class ShortUrlCachableServiceTest : BehaviorSpec({

    val shortUrlRepository = mockk<ShortUrlRepository>()
    val redisTemplate = mockk<RedisTemplate<String, String>>()
    val objectMapper = mockk<ObjectMapper>()
    val valueOperations = mockk<ValueOperations<String, String>>()
    val shortUrlCachableService = ShortUrlCachableService(shortUrlRepository, redisTemplate, objectMapper)

    beforeTest {
        every { redisTemplate.opsForValue() } returns valueOperations
    }

    afterTest {
        clearAllMocks()
    }

    Given("ShortUrlCachableService가 주어졌을 때") {
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
        val shortUrlJson = """{"shortKey":"$shortKey","originalUrl":"$originalUrl", "shortUrl":"$baseUrl/$shortKey", "createdAt":"$now", "expiresAt":"$expiresAt"}"""


        When("findShortUrlByShortKeyOrThrow 호출 시") {
            And("캐시에 ShortUrl이 존재하는 경우") {
                every { valueOperations.get("${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey") } returns shortUrlJson
                every { objectMapper.readValue(shortUrlJson, ShortUrl::class.java) } returns shortUrlDomain

                val result = shortUrlCachableService.findShortUrlByShortKeyOrThrow(shortKey)

                Then("캐시된 ShortUrl을 반환해야 한다") {
                    result.id shouldBe shortUrlDomain.id
                    verify(exactly = 1) { valueOperations.get("${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey") }
                    verify(exactly = 0) { shortUrlRepository.findByShortKey(any()) }
                    verify(exactly = 0) { valueOperations.set(any(), any(), any<java.time.Duration>()) }
                }
            }

            And("캐시에 ShortUrl이 존재하지 않고, DB에 존재하는 경우") {
                every { valueOperations.get("${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey") } returns null
                every { shortUrlRepository.findByShortKey(shortKey) } returns Optional.of(shortUrlEntity)
                every { objectMapper.writeValueAsString(any<ShortUrl>()) } returns shortUrlJson
                every { valueOperations.set(any(), any(), any<java.time.Duration>()) } returns Unit

                val result = shortUrlCachableService.findShortUrlByShortKeyOrThrow(shortKey)

                Then("DB에서 조회 후 캐시에 저장하고 ShortUrl을 반환해야 한다") {
                    result.id shouldBe shortUrlDomain.id
                    verify(exactly = 1) { valueOperations.get("${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey") }
                    verify(exactly = 1) { shortUrlRepository.findByShortKey(shortKey) }
                    verify(exactly = 1) { objectMapper.writeValueAsString(any<ShortUrl>()) }
                    verify(exactly = 1) { valueOperations.set(any(), any(), any<java.time.Duration>()) }
                }
            }

            And("캐시에도 DB에도 ShortUrl이 존재하지 않는 경우") {
                every { valueOperations.get("${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey") } returns null
                every { shortUrlRepository.findByShortKey(shortKey) } returns Optional.empty()

                Then("NoSuchElementException을 발생시켜야 한다") {
                    val exception = runCatching {
                        shortUrlCachableService.findShortUrlByShortKeyOrThrow(shortKey)
                    }.exceptionOrNull()

                    exception.shouldNotBeNull()
                    val noSuchElementException = exception as NoSuchElementException
                    noSuchElementException.message shouldContain "Short URL not found: $shortKey"
                    verify(exactly = 1) { valueOperations.get("${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey") }
                    verify(exactly = 1) { shortUrlRepository.findByShortKey(shortKey) }
                    verify(exactly = 0) { valueOperations.set(any(), any(), any<java.time.Duration>()) }
                }
            }
        }

        When("findShortUrlByOriginalUrlOrThrow 호출 시") {
            And("DB에 ShortUrl이 존재하는 경우") {
                every { shortUrlRepository.findByOriginalUrl(originalUrl) } returns Optional.of(shortUrlEntity)

                val result = shortUrlCachableService.findShortUrlByOriginalUrlOrThrow(originalUrl)

                Then("DB에서 조회한 ShortUrl을 반환해야 한다") {
                    result.id shouldBe shortUrlDomain.id
                    verify(exactly = 1) { shortUrlRepository.findByOriginalUrl(originalUrl) }
                }
            }

            And("DB에 ShortUrl이 존재하지 않는 경우") {
                every { shortUrlRepository.findByOriginalUrl(originalUrl) } returns Optional.empty()

                Then("NoSuchElementException을 발생시켜야 한다") {
                    val exception = runCatching {
                        shortUrlCachableService.findShortUrlByOriginalUrlOrThrow(originalUrl)
                    }.exceptionOrNull()

                    exception.shouldNotBeNull()
                    val noSuchElementException = exception as NoSuchElementException
                    noSuchElementException.message shouldContain "Short URL not found for original URL: $originalUrl"

                    verify(exactly = 1) { shortUrlRepository.findByOriginalUrl(originalUrl) }
                }
            }
        }
    }
})