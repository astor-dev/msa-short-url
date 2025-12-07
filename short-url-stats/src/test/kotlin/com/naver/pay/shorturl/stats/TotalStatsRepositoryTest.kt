package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsDocument
import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class TotalStatsRepositoryTest : BehaviorSpec({
    val shortUrlTotalStatsRepository = mockk<ShortUrlTotalStatsRepository>()
    val redisTemplate = mockk<RedisTemplate<String, String>>()
    val valueOperations = mockk<ValueOperations<String, String>>()
    val hashOperations = mockk<HashOperations<String, String, String>>()

    val totalStatsRepository = TotalStatsRepository(
        shortUrlTotalStatsRepository = shortUrlTotalStatsRepository,
        redisTemplate = redisTemplate
    )

    beforeTest {
        every { redisTemplate.opsForValue() } returns valueOperations
        every { redisTemplate.opsForHash<String, String>() } returns hashOperations
    }

    afterTest {
        clearAllMocks()
    }

    Given("recordClickToCache 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val date = LocalDate.of(2024, 1, 1)
        val device = "mobile"
        val referrer = "testReferrer"
        val clickedAt = Instant.now()

        When("정상적으로 실행하는 경우") {
            every {
                redisTemplate.execute<Any?>(any(), any(), *anyVararg<Any>())
            } returns Unit

            totalStatsRepository.recordClickToCache(shortKey, date, device, referrer, clickedAt)

            Then("Redis 스크립트를 실행해야 한다") {
                verify(exactly = 1) {
                    redisTemplate.execute(any(), any(), *anyVararg<Any>())
                }
            }
        }
    }

    Given("findOneFromDbAndInitializeCache 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val metadata = ShortUrlMetadata(
            shortUrl = "https://short.naver.com/testKey",
            originalUrl = "https://naver.com",
            shortUrlCreatedAt = Instant.now(),
            shortUrlExpiredAt = Instant.now().plusSeconds(3600)
        )
        val document = ShortUrlTotalStatsDocument(
            shortKey = shortKey,
            totalClicks = 100L,
            byDate = mutableMapOf("2024-01-01" to 50L, "2024-01-02" to 50L),
            byDevice = mutableMapOf("mobile" to 60L, "desktop" to 40L),
            byReferrer = mutableMapOf("referrer1" to 70L, "referrer2" to 30L),
            lastClickedAt = Instant.now(),
            metadata = metadata
        )

        When("데이터가 존재하는 경우") {
            every {
                shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
            } returns document
            every {
                valueOperations.set(any(), any())
            } returns Unit
            every {
                redisTemplate.expire(any<String>(), any<Duration>())
            } returns true
            every {
                hashOperations.putAll(any(), any())
            } returns Unit

            val result = totalStatsRepository.findOneFromDbAndInitializeCache(shortKey)

            Then("TotalStats를 반환하고 캐시를 초기화해야 한다") {
                result shouldBe document.toDomain()
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
                }
                verify(exactly = 1) {
                    valueOperations.set("${CacheNames.TOTAL_STATS_TOTAL_CLICKS}::$shortKey", "100")
                }
                verify(exactly = 1) {
                    hashOperations.putAll("${CacheNames.TOTAL_STATS_BY_DATE}::$shortKey", any())
                }
                verify(exactly = 1) {
                    hashOperations.putAll("${CacheNames.TOTAL_STATS_BY_DEVICE}::$shortKey", any())
                }
                verify(exactly = 1) {
                    hashOperations.putAll("${CacheNames.TOTAL_STATS_BY_REFERRER}::$shortKey", any())
                }
                verify(exactly = 1) {
                    valueOperations.set("${CacheNames.TOTAL_STATS_LAST_CLICKED_AT}::$shortKey", any())
                }
            }
        }

        When("데이터가 존재하지 않는 경우") {
            every {
                shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
            } returns null

            val result = totalStatsRepository.findOneFromDbAndInitializeCache(shortKey)

            Then("null을 반환해야 한다") {
                result shouldBe null
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
                }
                verify(exactly = 0) {
                    valueOperations.set(any(), any())
                }
            }
        }
    }

    Given("findTotalStatsInCache 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val totalClicksKey = "${CacheNames.TOTAL_STATS_TOTAL_CLICKS}::$shortKey"
        val byDateKey = "${CacheNames.TOTAL_STATS_BY_DATE}::$shortKey"
        val byDeviceKey = "${CacheNames.TOTAL_STATS_BY_DEVICE}::$shortKey"
        val byReferrerKey = "${CacheNames.TOTAL_STATS_BY_REFERRER}::$shortKey"
        val lastClickedAtKey = "${CacheNames.TOTAL_STATS_LAST_CLICKED_AT}::$shortKey"

        When("캐시에 데이터가 존재하는 경우") {
            val clickedAt = Instant.now()

            every { redisTemplate.hasKey(totalClicksKey) } returns true
            every { valueOperations.get(totalClicksKey) } returns "100"
            every {
                hashOperations.entries(byDateKey)
            } returns mapOf("2024-01-01" to "50", "2024-01-02" to "50")
            every {
                hashOperations.entries(byDeviceKey)
            } returns mapOf("mobile" to "60", "desktop" to "40")
            every {
                hashOperations.entries(byReferrerKey)
            } returns mapOf("referrer1" to "70", "referrer2" to "30")
            every { valueOperations.get(lastClickedAtKey) } returns clickedAt.toString()

            val result = totalStatsRepository.findTotalStatsInCache(shortKey)

            Then("TotalStatsVo를 반환해야 한다") {
                result shouldBe TotalStatsVo(
                    shortKey = shortKey,
                    totalClicks = 100L,
                    byDate = listOf(
                        DateCountVo(date = "2024-01-01", clicks = 50L),
                        DateCountVo(date = "2024-01-02", clicks = 50L)
                    ),
                    byDevice = listOf(
                        DeviceCountVo(deviceType = "mobile", clicks = 60L),
                        DeviceCountVo(deviceType = "desktop", clicks = 40L)
                    ),
                    byReferrer = listOf(
                        ReferrerCountVo(referrer = "referrer1", clicks = 70L),
                        ReferrerCountVo(referrer = "referrer2", clicks = 30L)
                    ),
                    lastClickedAt = clickedAt
                )
                verify(exactly = 1) { redisTemplate.hasKey(totalClicksKey) }
                verify(exactly = 1) { valueOperations.get(totalClicksKey) }
                verify(exactly = 1) { hashOperations.entries(byDateKey) }
                verify(exactly = 1) { hashOperations.entries(byDeviceKey) }
                verify(exactly = 1) { hashOperations.entries(byReferrerKey) }
                verify(exactly = 1) { valueOperations.get(lastClickedAtKey) }
            }
        }

        When("캐시에 데이터가 존재하지 않는 경우") {
            every { redisTemplate.hasKey(totalClicksKey) } returns false

            val result = totalStatsRepository.findTotalStatsInCache(shortKey)

            Then("null을 반환해야 한다") {
                result shouldBe null
                verify(exactly = 1) { redisTemplate.hasKey(totalClicksKey) }
                verify(exactly = 0) { valueOperations.get(any()) }
                verify(exactly = 0) { hashOperations.entries(any()) }
            }
        }

        When("Hash의 값이 숫자로 변환되지 않는 경우") {
            every { redisTemplate.hasKey(totalClicksKey) } returns true
            every { valueOperations.get(totalClicksKey) } returns "100"
            every {
                hashOperations.entries(byDateKey)
            } returns mapOf("2024-01-01" to "invalid")
            every {
                hashOperations.entries(byDeviceKey)
            } returns emptyMap()
            every {
                hashOperations.entries(byReferrerKey)
            } returns emptyMap()
            every { valueOperations.get(lastClickedAtKey) } returns null

            val result = totalStatsRepository.findTotalStatsInCache(shortKey)

            Then("clicks는 0으로 설정되어야 한다") {
                result shouldBe TotalStatsVo(
                    shortKey = shortKey,
                    totalClicks = 100L,
                    byDate = listOf(DateCountVo(date = "2024-01-01", clicks = 0L)),
                    byDevice = emptyList(),
                    byReferrer = emptyList(),
                    lastClickedAt = null
                )
            }
        }

        When("lastClickedAt이 없는 경우") {
            every { redisTemplate.hasKey(totalClicksKey) } returns true
            every { valueOperations.get(totalClicksKey) } returns "100"
            every {
                hashOperations.entries(byDateKey)
            } returns emptyMap()
            every {
                hashOperations.entries(byDeviceKey)
            } returns emptyMap()
            every {
                hashOperations.entries(byReferrerKey)
            } returns emptyMap()
            every { valueOperations.get(lastClickedAtKey) } returns null

            val result = totalStatsRepository.findTotalStatsInCache(shortKey)

            Then("lastClickedAt은 null이어야 한다") {
                result?.lastClickedAt shouldBe null
            }
        }
    }

    Given("save 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val metadata = ShortUrlMetadata(
            shortUrl = "https://short.naver.com/testKey",
            originalUrl = "https://naver.com",
            shortUrlCreatedAt = Instant.now(),
            shortUrlExpiredAt = Instant.now().plusSeconds(3600)
        )
        val totalStats = TotalStats(
            shortKey = shortKey,
            totalClicks = 100L,
            byDate = listOf(
                ShortUrlStatsByDate(date = "2024-01-01", clicks = 50L),
                ShortUrlStatsByDate(date = "2024-01-02", clicks = 50L)
            ),
            byDevice = listOf(
                ShortUrlStatsByDevice(deviceType = "mobile", clicks = 60L),
                ShortUrlStatsByDevice(deviceType = "desktop", clicks = 40L)
            ),
            byReferrer = listOf(
                ShortUrlStatsByReferrer(referrer = "referrer1", clicks = 70L),
                ShortUrlStatsByReferrer(referrer = "referrer2", clicks = 30L)
            ),
            lastClickedAt = Instant.now(),
            metadata = metadata
        )

        When("정상적으로 저장하는 경우") {
            every {
                shortUrlTotalStatsRepository.save(any<ShortUrlTotalStatsDocument>())
            } returns ShortUrlTotalStatsDocument(
                shortKey = shortKey,
                totalClicks = 100L,
                byDate = mutableMapOf(),
                byDevice = mutableMapOf(),
                byReferrer = mutableMapOf(),
                lastClickedAt = null,
                metadata = metadata
            )

            totalStatsRepository.save(totalStats)

            Then("MongoDB에 저장해야 한다") {
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.save(any<ShortUrlTotalStatsDocument>())
                }
            }
        }
    }
})

