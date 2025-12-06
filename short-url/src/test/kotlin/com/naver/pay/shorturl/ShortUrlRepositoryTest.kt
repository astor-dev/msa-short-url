package com.naver.pay.shorturl

import com.naver.pay.outbox.OutboxService
import com.naver.pay.shorturl.exception.ExpiredLinkException
import com.naver.pay.shorturl.jpa.ShortUrlEntity
import com.naver.pay.shorturl.jpa.ShortUrlJpaRepository
import com.naver.pay.shorturl.stream.Bindings
import com.naver.pay.shorturl.stream.ShortUrlEventProducer
import com.naver.pay.util.DistributedLockExecutor
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
import java.time.temporal.ChronoUnit
import java.util.Optional

class ShortUrlRepositoryTest: BehaviorSpec ({
    val redisTemplate = mockk<RedisTemplate<String, String>>()
    val valueOperations = mockk<ValueOperations<String, String>>()
    val outboxService = mockk<OutboxService>()
    val shortUrlJpaRepository = mockk<ShortUrlJpaRepository>()
    val distributedLockExecutor = mockk<DistributedLockExecutor>()
    val shortUrlEventProducer = mockk<ShortUrlEventProducer>()
    val shortUrlRepository = ShortUrlRepository(
        redisTemplate = redisTemplate,
        outboxService = outboxService,
        shortUrlJpaRepository = shortUrlJpaRepository,
        distributedLockExecutor = distributedLockExecutor,
        shortUrlEventProducer = shortUrlEventProducer
    )

    beforeTest {
        every { redisTemplate.opsForValue() } returns valueOperations
    }

    afterTest {
        clearAllMocks()
    }

    Given("getRedirectUrl 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val originalUrl = "https://naver.com"
        val baseUrl = "http://localhost"
        val userAgent = "test-agent"
        val referrer = "test-referrer"
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val expiresAt = now.plusSeconds(86400)
        val redirectUrlCacheKey = "${CacheNames.REDIRECT_URL_BY_SHORT_KEY}::$shortKey"
        val shortUrlEntity = ShortUrlEntity(
            id = 1L,
            shortKey = shortKey,
            baseUrl = baseUrl,
            originalUrl = originalUrl,
            expiresAt = expiresAt,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )

        When("캐시에 redirectUrl이 존재하는 경우") {
            every { valueOperations.get(redirectUrlCacheKey) } returns originalUrl
            every {
                shortUrlEventProducer.publishUrlClicked(
                    shortKey = shortKey,
                    referrer = referrer,
                    userAgent = userAgent
                )
            } returns Unit

            val result = shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)

            Then("캐시된 redirectUrl을 반환하고 이벤트를 발행해야 한다") {
                result shouldBe originalUrl
                verify(exactly = 1) { valueOperations.get(redirectUrlCacheKey) }
                verify(exactly = 0) {
                    distributedLockExecutor.execute(
                        lockName = any(),
                        key = any(),
                        block = any()
                    )
                }
                verify(exactly = 1) {
                    shortUrlEventProducer.publishUrlClicked(
                        shortKey = shortKey,
                        referrer = referrer,
                        userAgent = userAgent
                    )
                }
            }
        }

        When("캐시에 redirectUrl이 없고, 락 획득 후 캐시 재확인 시에도 없고, DB에 존재하는 경우") {
            every { valueOperations.get(redirectUrlCacheKey) } returns null
            every { shortUrlJpaRepository.findByShortKey(shortKey) } returns Optional.of(shortUrlEntity)
            every { valueOperations.set(any(), any(), any<Duration>()) } returns Unit
            every {
                distributedLockExecutor.execute<String?>(
                    lockName = CacheNames.SHORT_URL_GET_LOCK,
                    key = shortKey,
                    block = any()
                )
            } answers {
                val block = lastArg<() -> String?>()
                block.invoke()
            }
            every {
                shortUrlEventProducer.publishUrlClicked(
                    shortKey = shortKey,
                    referrer = referrer,
                    userAgent = userAgent
                )
            } returns Unit

            val result = shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)

            Then("DB에서 조회 후 캐시에 저장하고 redirectUrl을 반환하고 이벤트를 발행해야 한다") {
                result shouldBe originalUrl
                verify(exactly = 2) { valueOperations.get(redirectUrlCacheKey) }
                verify(exactly = 1) {
                    distributedLockExecutor.execute(
                        lockName = CacheNames.SHORT_URL_GET_LOCK,
                        key = shortKey,
                        block = any()
                    )
                }
                verify(exactly = 1) { shortUrlJpaRepository.findByShortKey(shortKey) }
                verify(exactly = 1) { valueOperations.set(redirectUrlCacheKey, originalUrl, any<Duration>()) }
                verify(exactly = 1) {
                    shortUrlEventProducer.publishUrlClicked(
                        shortKey = shortKey,
                        referrer = referrer,
                        userAgent = userAgent
                    )
                }
            }
        }

        When("캐시에 redirectUrl이 없고, 락 획득 후 캐시 재확인 시에는 있는 경우") {
            every { valueOperations.get(redirectUrlCacheKey) } returnsMany listOf(null, originalUrl)
            every {
                distributedLockExecutor.execute<String?>(
                    lockName = CacheNames.SHORT_URL_GET_LOCK,
                    key = shortKey,
                    block = any()
                )
            } answers {
                val block = lastArg<() -> String?>()
                block.invoke()
            }
            every {
                shortUrlEventProducer.publishUrlClicked(
                    shortKey = shortKey,
                    referrer = referrer,
                    userAgent = userAgent
                )
            } returns Unit

            val result = shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)

            Then("락 획득 후 캐시에서 조회한 redirectUrl을 반환하고 이벤트를 발행해야 한다") {
                result shouldBe originalUrl
                verify(exactly = 2) { valueOperations.get(redirectUrlCacheKey) }
                verify(exactly = 1) {
                    distributedLockExecutor.execute(
                        lockName = CacheNames.SHORT_URL_GET_LOCK,
                        key = shortKey,
                        block = any()
                    )
                }
                verify(exactly = 0) { shortUrlJpaRepository.findByShortKey(any()) }
                verify(exactly = 0) { valueOperations.set(any(), any(), any<Duration>()) }
                verify(exactly = 1) {
                    shortUrlEventProducer.publishUrlClicked(
                        shortKey = shortKey,
                        referrer = referrer,
                        userAgent = userAgent
                    )
                }
            }
        }

        When("캐시에도 DB에도 ShortUrl이 존재하지 않는 경우") {
            every { valueOperations.get(redirectUrlCacheKey) } returns null
            every { shortUrlJpaRepository.findByShortKey(shortKey) } returns Optional.empty()
            every {
                distributedLockExecutor.execute<String?>(
                    lockName = CacheNames.SHORT_URL_GET_LOCK,
                    key = shortKey,
                    block = any()
                )
            } answers {
                val block = lastArg<() -> String?>()
                block.invoke()
            }

            val result = shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)

            Then("null을 반환하고 이벤트를 발행하지 않아야 한다") {
                result shouldBe null
                verify(exactly = 2) { valueOperations.get(redirectUrlCacheKey) }
                verify(exactly = 1) {
                    distributedLockExecutor.execute<String?>(
                        lockName = CacheNames.SHORT_URL_GET_LOCK,
                        key = shortKey,
                        block = any()
                    )
                }
                verify(exactly = 1) { shortUrlJpaRepository.findByShortKey(shortKey) }
                verify(exactly = 0) { valueOperations.set(any(), any(), any<Duration>()) }
                verify(exactly = 0) {
                    shortUrlEventProducer.publishUrlClicked(any(), any(), any())
                }
            }
        }

        When("만료된 링크인 경우") {
            val expiredEntity = ShortUrlEntity(
                id = 1L,
                shortKey = shortKey,
                baseUrl = baseUrl,
                originalUrl = originalUrl,
                expiresAt = now.minusSeconds(3600),
                createdAt = now.minusSeconds(7200),
                updatedAt = now,
                deletedAt = null
            )
            every { valueOperations.get(redirectUrlCacheKey) } returns null
            every { shortUrlJpaRepository.findByShortKey(shortKey) } returns Optional.of(expiredEntity)
            every {
                distributedLockExecutor.execute<String?>(
                    lockName = CacheNames.SHORT_URL_GET_LOCK,
                    key = shortKey,
                    block = any()
                )
            } answers {
                val block = lastArg<() -> String?>()
                block.invoke()
            }

            Then("ExpiredLinkException을 던지고 이벤트를 발행하지 않아야 한다") {
                shouldThrow<ExpiredLinkException> {
                    shortUrlRepository.getRedirectUrl(shortKey, userAgent, referrer)
                }
                verify(exactly = 0) {
                    shortUrlEventProducer.publishUrlClicked(any(), any(), any())
                }
            }
        }

        When("userAgent와 referrer가 null인 경우") {
            every { valueOperations.get(redirectUrlCacheKey) } returns originalUrl
            every {
                shortUrlEventProducer.publishUrlClicked(
                    shortKey = shortKey,
                    referrer = "Direct",
                    userAgent = "Unknown"
                )
            } returns Unit

            val result = shortUrlRepository.getRedirectUrl(shortKey, null, null)

            Then("기본값으로 이벤트를 발행해야 한다") {
                result shouldBe originalUrl
                verify(exactly = 1) {
                    shortUrlEventProducer.publishUrlClicked(
                        shortKey = shortKey,
                        referrer = "Direct",
                        userAgent = "Unknown"
                    )
                }
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