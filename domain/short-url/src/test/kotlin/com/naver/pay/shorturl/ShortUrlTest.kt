package com.naver.pay.shorturl

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class ShortUrlTest : BehaviorSpec({

    Given("ShortUrl 생성을 위한 유효한 파라미터가 주어졌을 때") {
        val shortKey = "testKey"
        val baseUrl = "https://short.url"
        val originalUrl = "https://naver.com/index.html"
        val ttlSeconds = 3600

        When("of 팩토리 메소드를 호출하면") {
            val beforeCreation = Instant.now()
            val shortUrlInstance = ShortUrl.of(
                shortKey = shortKey,
                baseUrl = baseUrl,
                originalUrl = originalUrl,
                ttlSeconds = ttlSeconds
            )
            val afterCreation = Instant.now()

            Then("모든 필드가 정상적으로 초기화된다") {
                shortUrlInstance.shortKey shouldBe shortKey
                shortUrlInstance.baseUrl shouldBe baseUrl
                shortUrlInstance.originalUrl shouldBe originalUrl
            }

            Then("생성 시각과 만료 시각이 올바르게 설정된다") {
                shortUrlInstance.createdAt.isAfter(beforeCreation.minusMillis(100)) shouldBe true
                shortUrlInstance.createdAt.isBefore(afterCreation.plusMillis(100)) shouldBe true
                shortUrlInstance.expiresAt shouldBe shortUrlInstance.createdAt.plusSeconds(ttlSeconds.toLong())
            }
        }
    }

    Given("ShortUrl 생성을 위한 유효하지 않은 파라미터가 주어졌을 때") {
        When("shortKey가 비어 있으면") {
            Then("IllegalArgumentException 예외가 발생한다") {
                val exception = assertThrows<IllegalArgumentException> {
                    ShortUrl.of("", "https://short.url", "https://naver.com", 3600)
                }
                exception.message shouldBe "shortKey는 비어 있을 수 없습니다."
            }
        }

        When("baseUrl이 비어 있으면") {
            Then("IllegalArgumentException 예외가 발생한다") {
                val exception = assertThrows<IllegalArgumentException> {
                    ShortUrl.of("testKey", "", "https://naver.com", 3600)
                }
                exception.message shouldBe "baseUrl 비어 있을 수 없습니다."
            }
        }

        When("originalUrl이 비어 있으면") {
            Then("IllegalArgumentException 예외가 발생한다") {
                val exception = assertThrows<IllegalArgumentException> {
                    ShortUrl.of("testKey", "https://short.url", "", 3600)
                }
                exception.message shouldBe "originalUrl는 비어 있을 수 없습니다."
            }
        }

        When("ttlSeconds가 0이거나 음수이면") {
            Then("IllegalArgumentException 예외가 발생한다") {
                val exception1 = assertThrows<IllegalArgumentException> {
                    ShortUrl.of("testKey", "https://short.url", "https://naver.com", 0)
                }
                exception1.message shouldBe "expiresAt은 createdAt 이후여야 합니다."

                val exception2 = assertThrows<IllegalArgumentException> {
                    ShortUrl.of("testKey", "https://short.url", "https://naver.com", -100)
                }
                exception2.message shouldBe "expiresAt은 createdAt 이후여야 합니다."
            }
        }
    }

    Given("ShortUrl 재구성을 위한 유효한 파라미터가 주어졌을 때") {
        val id = 1L
        val shortKey = "testKey"
        val baseUrl = "https://short.url"
        val originalUrl = "https://naver.com/index.html"
        val createdAt = Instant.now().minusSeconds(3600)
        val expiresAt = Instant.now().plusSeconds(3600)

        When("of 팩토리 메소드를 호출하면") {
            val shortUrlInstance = ShortUrl.of(
                id = id,
                shortKey = shortKey,
                baseUrl = baseUrl,
                originalUrl = originalUrl,
                createdAt = createdAt,
                expiresAt = expiresAt
            )

            Then("모든 필드가 정상적으로 초기화된다") {
                shortUrlInstance.id shouldBe id
                shortUrlInstance.shortKey shouldBe shortKey
                shortUrlInstance.baseUrl shouldBe baseUrl
                shortUrlInstance.originalUrl shouldBe originalUrl
                shortUrlInstance.createdAt shouldBe createdAt
                shortUrlInstance.expiresAt shouldBe expiresAt
            }
        }

        When("expiresAt이 createdAt과 동일하거나 이전이면") {
            val createdAt = Instant.now()

            Then("IllegalArgumentException 예외가 발생한다") {
                val exception1 = assertThrows<IllegalArgumentException> {
                    ShortUrl.of(1L, "testKey", "https://short.url", "https://naver.com", createdAt, createdAt)
                }
                exception1.message shouldBe "expiresAt은 createdAt 이후여야 합니다."

                val exception2 = assertThrows<IllegalArgumentException> {
                    ShortUrl.of(1L, "testKey", "https://short.url", "https://naver.com", createdAt, createdAt.minusSeconds(100))
                }
                exception2.message shouldBe "expiresAt은 createdAt 이후여야 합니다."
            }
        }
    }
})