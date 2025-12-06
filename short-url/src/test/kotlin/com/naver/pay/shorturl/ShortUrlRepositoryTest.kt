package com.naver.pay.shorturl

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.outbox.OutboxService
import com.naver.pay.shorturl.jpa.ShortUrlEntity
import com.naver.pay.shorturl.jpa.ShortUrlJpaRepository
import com.naver.pay.shorturl.stream.Bindings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.time.Instant

class ShortUrlRepositoryTest: BehaviorSpec ({
    val redisTemplate = mockk<RedisTemplate<String, String>>()
    val objectMapper = mockk<ObjectMapper>()
    val valueOperations = mockk<ValueOperations<String, String>>()
    val outboxService = mockk<OutboxService>()
    val shortUrlJpaRepository = mockk< ShortUrlJpaRepository>()
    val shortUrlRepository = ShortUrlRepository(redisTemplate, objectMapper, outboxService, shortUrlJpaRepository)

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

            val result = shortUrlRepository.findShortUrlByShortKeyInCache(shortKey)

            Then("역직렬화된 ShortUrl 객체를 반환한다") {
                result shouldBe shortUrl
                verify(exactly = 1) { valueOperations.get(cacheKey) }
                verify(exactly = 1) { objectMapper.readValue(shortUrlJson, ShortUrl::class.java) }
            }
        }

        When("캐시에 값이 존재하지 않을 경우") {
            every { valueOperations.get(cacheKey) } returns null

            val result = shortUrlRepository.findShortUrlByShortKeyInCache(shortKey)

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

            val result = shortUrlRepository.findShortUrlByShortKeyInCache(shortKey)

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

            shortUrlRepository.cacheShortUrlByShortKey(shortUrl, ttl)

            Then("Redis에 값을 저장해야 한다") {
                verify(exactly = 1) { objectMapper.writeValueAsString(shortUrl) }
                verify(exactly = 1) { valueOperations.set(cacheKey, shortUrlJson, ttl) }
            }
        }
    }

    Given("createShortUrl 메소드가 주어졌을 때") {
        val baseUrl = "http://localhost"
        val originalUrl = "https://naver.com"
        val ttlSeconds = 3600
        val createdAt = Instant.now()
        val expiresAt = createdAt.plusSeconds(ttlSeconds.toLong())
        val savedEntityId = 1L
        val savedEntity = ShortUrlEntity(
            id = savedEntityId,
            shortKey = "temp",
            baseUrl = baseUrl,
            originalUrl = originalUrl,
            expiresAt = expiresAt,
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null
        )
        val updatedShortKey = ShortUrl.generateShortKey(savedEntityId)
        val updatedEntity = ShortUrlEntity(
            id = savedEntityId,
            shortKey = updatedShortKey,
            baseUrl = baseUrl,
            originalUrl = originalUrl,
            expiresAt = expiresAt,
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null
        )

        When("정상적으로 저장되는 경우") {
            every { shortUrlJpaRepository.save(any<ShortUrlEntity>()) } returnsMany listOf(savedEntity, updatedEntity)
            every { outboxService.storeEvent(any(), any()) } returns Unit

            val result = shortUrlRepository.createShortUrl(baseUrl, originalUrl, ttlSeconds)

            Then("ShortUrl을 생성하고 저장한다") {
                result.shortKey shouldBe updatedShortKey
                result.originalUrl shouldBe originalUrl
                verify(exactly = 2) { shortUrlJpaRepository.save(any<ShortUrlEntity>()) }
                verify(exactly = 1) {
                    outboxService.storeEvent(
                        Bindings.SHORT_URL_CREATED,
                        any()
                    )
                }
            }
        }

        When("UUID 충돌이 발생하고 재시도 후 성공하는 경우") {
            val conflictException = DataIntegrityViolationException("Unique constraint violation")
            every {
                shortUrlJpaRepository.save(any())
            } answers {
                throw conflictException
            } andThenAnswer {
                throw conflictException
            } andThenAnswer {
                savedEntity
            } andThenAnswer {
                updatedEntity
            }

            every { outboxService.storeEvent(any(), any()) } returns Unit

            val result = shortUrlRepository.createShortUrl(baseUrl, originalUrl, ttlSeconds)

            Then("재시도 후 성공적으로 저장한다") {
                result.shortKey shouldBe updatedShortKey
                verify(exactly = 4) { shortUrlJpaRepository.save(any<ShortUrlEntity>()) }
            }
        }

        When("최대 재시도 횟수를 초과하는 경우") {
            val conflictException = DataIntegrityViolationException("Unique constraint violation")
            every { shortUrlJpaRepository.save(any<ShortUrlEntity>()) } throws conflictException

            Then("DataIntegrityViolationException을 던진다") {
                val exception = shouldThrow<DataIntegrityViolationException> {
                    shortUrlRepository.createShortUrl(baseUrl, originalUrl, ttlSeconds)
                }
                exception.message shouldBe "shortKey 생성 실패: 최대 재시도 횟수(3)를 초과했습니다. 원인: ${conflictException.message}"
                verify(exactly = 3) { shortUrlJpaRepository.save(any<ShortUrlEntity>()) }
            }
        }
    }
})