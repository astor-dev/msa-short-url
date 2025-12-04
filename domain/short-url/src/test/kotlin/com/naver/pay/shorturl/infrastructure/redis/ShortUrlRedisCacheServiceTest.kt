package com.naver.pay.shorturl.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.shorturl.CacheNames
import com.naver.pay.shorturl.ShortUrl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.time.Instant

class ShortUrlRedisCacheServiceTest : BehaviorSpec({
    val redisTemplate = mockk<RedisTemplate<String, String>>()
    val objectMapper = mockk<ObjectMapper>()
    val valueOperations = mockk<ValueOperations<String, String>>()

    val shortUrlRedisCacheService = ShortUrlRedisCacheService(redisTemplate, objectMapper)

    beforeTest {
        every { redisTemplate.opsForValue() } returns valueOperations
    }

    afterTest {
        clearAllMocks()
    }

    Given("findShortUrlByShortKey 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val cacheKey = "${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey"
        val createdAt = Instant.now()
        val expiresAt = createdAt.plusSeconds(3600)
        val shortUrl = ShortUrl.of(
            id = 1,
            originalUrl = "https://naver.com",
            shortKey = shortKey,
            baseUrl = "http://localhost",
            createdAt = createdAt,
            expiresAt = expiresAt
        )
        val shortUrlJson = """{"id":null,"shortKey":"testKey","originalUrl":"https://naver.com","shortUrl":"http://localhost/testKey","createdAt":$createdAt,"expiresAt":$expiresAt}"""


        When("캐시에 값이 존재하고 정상적인 JSON일 경우") {
            every { valueOperations.get(cacheKey) } returns shortUrlJson
            every { objectMapper.readValue(shortUrlJson, ShortUrl::class.java) } returns shortUrl

            val result = shortUrlRedisCacheService.findShortUrlByShortKey(shortKey)

            Then("역직렬화된 ShortUrl 객체를 반환한다") {
                result shouldBe shortUrl
                verify(exactly = 1) { valueOperations.get(cacheKey) }
                verify(exactly = 1) { objectMapper.readValue(shortUrlJson, ShortUrl::class.java) }
            }
        }

        When("캐시에 값이 존재하지 않을 경우") {
            every { valueOperations.get(cacheKey) } returns null

            val result = shortUrlRedisCacheService.findShortUrlByShortKey(shortKey)

            Then("null을 반환한다") {
                result shouldBe null
                verify(exactly = 1) { valueOperations.get(cacheKey) }
                verify(exactly = 0) { objectMapper.readValue(cacheKey, eq(ShortUrl::class.java)) }
            }
        }

        When("캐시의 값이 비정상적인 JSON일 경우") {
            val invalidJson = "invalid json"
            every { valueOperations.get(cacheKey) } returns invalidJson
            every { objectMapper.readValue(invalidJson, ShortUrl::class.java) } throws Exception()

            val result = shortUrlRedisCacheService.findShortUrlByShortKey(shortKey)

            Then("null을 반환한다") {
                result shouldBe null
                verify(exactly = 1) { valueOperations.get(cacheKey) }
                verify(exactly = 1) { objectMapper.readValue(invalidJson, ShortUrl::class.java) }
            }
        }
    }

    Given("cacheShortUrlByShortKey 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val createdAt = Instant.now()
        val expiresAt = createdAt.plusSeconds(3600)
        val shortUrl = ShortUrl.of(
            id = 1,
            originalUrl = "https://naver.com",
            shortKey = shortKey,
            baseUrl = "http://localhost",
            createdAt = createdAt,
            expiresAt = expiresAt
        )
        val cacheKey = "${CacheNames.SHORT_URL_BY_SHORT_KEY}::$shortKey"
        val ttl = Duration.ofMinutes(10)
        val shortUrlJson = """{"id":1,"shortKey":"testKey","originalUrl":"https://naver.com","shortUrl":"http://localhost/testKey","createdAt":"${shortUrl.createdAt}","expiresAt":"${shortUrl.expiresAt}"}"""

        When("유효한 ShortUrl과 TTL이 주어지면") {
            every { objectMapper.writeValueAsString(shortUrl) } returns shortUrlJson
            every { valueOperations.set(cacheKey, shortUrlJson, ttl) } returns Unit

            shortUrlRedisCacheService.cacheShortUrlByShortKey(shortUrl, ttl)

            Then("Redis에 값을 저장해야 한다") {
                verify(exactly = 1) { objectMapper.writeValueAsString(shortUrl) }
                verify(exactly = 1) { valueOperations.set(cacheKey, shortUrlJson, ttl) }
            }
        }

        When("shortKey가 없는 ShortUrl이 주어지면") {
            val createdAt = Instant.now()
            val expiresAt = createdAt.plusSeconds(3600)
            val shortUrlWithNoKey = ShortUrl.of(
                id = 1,
                originalUrl = "https://naver.com",
                shortKey = null,
                baseUrl = "http://localhost",
                createdAt = createdAt,
                expiresAt = expiresAt
            )

            Then("IllegalStateException을 던진다") {
                shouldThrow<IllegalStateException> {
                    shortUrlRedisCacheService.cacheShortUrlByShortKey(shortUrlWithNoKey, ttl)
                }
                verify(exactly = 0) { objectMapper.writeValueAsString(any()) }
                verify(exactly = 0) { valueOperations.set("anyKey", "anyValue", 1L) }
            }
        }
    }
})
