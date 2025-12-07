package com.naver.pay.shorturl.stats

import com.mongodb.DuplicateKeyException
import com.mongodb.ServerAddress
import com.mongodb.WriteConcernResult
import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsDocument
import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bson.BsonDocument
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant
import java.time.LocalDate

class TotalStatsServiceTest : BehaviorSpec({
    val shortUrlTotalStatsRepository = mockk<ShortUrlTotalStatsRepository>()
    val totalStatsRepository = mockk<TotalStatsRepository>()
    val totalStatsService = TotalStatsService(
        shortUrlTotalStatsRepository = shortUrlTotalStatsRepository,
        totalStatsRepository = totalStatsRepository
    )

    afterTest {
        clearAllMocks()
    }

    Given("createTotalStatsIfNotExists 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val metadata = ShortUrlMetadata(
            shortUrl = "https://short.naver.com/testKey",
            originalUrl = "https://naver.com",
            shortUrlCreatedAt = Instant.now(),
            shortUrlExpiredAt = Instant.now().plusSeconds(3600)
        )

        When("정상적으로 생성하는 경우") {
            every {
                shortUrlTotalStatsRepository.insert(any<ShortUrlTotalStatsDocument>())
            } returns mockk<ShortUrlTotalStatsDocument>()

            totalStatsService.createTotalStatsIfNotExists(shortKey, metadata)

            Then("Document를 insert해야 한다") {
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.insert(any<ShortUrlTotalStatsDocument>())
                }
            }
        }

        When("중복 키 예외가 발생하는 경우") {
            val response = BsonDocument() // 최소 문서
            val address = ServerAddress("localhost", 27017)
            val writeConcernResult = WriteConcernResult.acknowledged(1, false, null)
            val ex = DuplicateKeyException(response, address, writeConcernResult)

            every {
                shortUrlTotalStatsRepository.insert(any<ShortUrlTotalStatsDocument>())
            } throws ex

            totalStatsService.createTotalStatsIfNotExists(shortKey, metadata)

            Then("예외를 무시하고 계속 진행해야 한다") {
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.insert(any<ShortUrlTotalStatsDocument>())
                }
            }
        }
    }

    Given("captureClick 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val referrer = "testReferrer"
        val device = "mobile"
        val date = LocalDate.of(2024, 1, 1)
        val clickedAt = Instant.now()

        When("정상적으로 실행하는 경우") {
            every {
                totalStatsRepository.recordClickToCache(shortKey, date, device, referrer, clickedAt)
            } returns Unit

            totalStatsService.captureClick(shortKey, referrer, device, date, clickedAt)

            Then("Repository의 recordClickToCache를 호출해야 한다") {
                verify(exactly = 1) {
                    totalStatsRepository.recordClickToCache(shortKey, date, device, referrer, clickedAt)
                }
            }
        }
    }

    Given("findOne 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val metadata = ShortUrlMetadata(
            shortUrl = "https://short.naver.com/testKey",
            originalUrl = "https://naver.com",
            shortUrlCreatedAt = Instant.now(),
            shortUrlExpiredAt = Instant.now().plusSeconds(3600)
        )

        When("캐시에 데이터가 존재하는 경우") {
            val totalStatsVo = TotalStatsVo(
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
                lastClickedAt = Instant.now()
            )

            val shortUrlDocument = ShortUrlTotalStatsDocument(
                shortKey = shortKey,
                totalClicks = 100L,
                byDate = mutableMapOf(),
                byDevice = mutableMapOf(),
                byReferrer = mutableMapOf(),
                lastClickedAt = null,
                metadata = metadata
            )

            every {
                totalStatsRepository.findTotalStatsInCache(shortKey)
            } returns totalStatsVo
            every {
                shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
            } returns shortUrlDocument

            val result = totalStatsService.findOne(shortKey)

            Then("캐시 데이터를 resolve하여 TotalStats를 반환해야 한다") {
                result shouldBe TotalStats(
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
                    lastClickedAt = totalStatsVo.lastClickedAt,
                    metadata = metadata
                )
                verify(exactly = 1) {
                    totalStatsRepository.findTotalStatsInCache(shortKey)
                }
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
                }
                verify(exactly = 0) {
                    totalStatsRepository.findOneFromDbAndInitializeCache(any())
                }
            }
        }

        When("캐시에 데이터가 없고 MongoDB에 데이터가 존재하는 경우") {
            val totalStats = TotalStats(
                shortKey = shortKey,
                totalClicks = 100L,
                byDate = listOf(
                    ShortUrlStatsByDate(date = "2024-01-01", clicks = 50L)
                ),
                byDevice = listOf(
                    ShortUrlStatsByDevice(deviceType = "mobile", clicks = 60L)
                ),
                byReferrer = listOf(
                    ShortUrlStatsByReferrer(referrer = "referrer1", clicks = 70L)
                ),
                lastClickedAt = Instant.now(),
                metadata = metadata
            )

            every {
                totalStatsRepository.findTotalStatsInCache(shortKey)
            } returns null
            every {
                totalStatsRepository.findOneFromDbAndInitializeCache(shortKey)
            } returns totalStats

            val result = totalStatsService.findOne(shortKey)

            Then("MongoDB 데이터를 반환해야 한다") {
                result shouldBe totalStats
                verify(exactly = 1) {
                    totalStatsRepository.findTotalStatsInCache(shortKey)
                }
                verify(exactly = 1) {
                    totalStatsRepository.findOneFromDbAndInitializeCache(shortKey)
                }
                verify(exactly = 0) {
                    shortUrlTotalStatsRepository.findByIdOrNull(any())
                }
            }
        }

        When("캐시에 데이터가 없고 MongoDB에도 데이터가 없는 경우") {
            every {
                totalStatsRepository.findTotalStatsInCache(shortKey)
            } returns null
            every {
                totalStatsRepository.findOneFromDbAndInitializeCache(shortKey)
            } returns null

            val result = totalStatsService.findOne(shortKey)

            Then("null을 반환해야 한다") {
                result shouldBe null
                verify(exactly = 1) {
                    totalStatsRepository.findTotalStatsInCache(shortKey)
                }
                verify(exactly = 1) {
                    totalStatsRepository.findOneFromDbAndInitializeCache(shortKey)
                }
            }
        }
    }

    Given("resolveTotalStats 메소드가 주어졌을 때") {
        val shortKey = "testKey"
        val metadata = ShortUrlMetadata(
            shortUrl = "https://short.naver.com/testKey",
            originalUrl = "https://naver.com",
            shortUrlCreatedAt = Instant.now(),
            shortUrlExpiredAt = Instant.now().plusSeconds(3600)
        )
        val totalStatsVo = TotalStatsVo(
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
            lastClickedAt = Instant.now()
        )

        When("shortKey에 해당하는 document가 존재하는 경우") {
            val shortUrlDocument = ShortUrlTotalStatsDocument(
                shortKey = shortKey,
                totalClicks = 100L,
                byDate = mutableMapOf(),
                byDevice = mutableMapOf(),
                byReferrer = mutableMapOf(),
                lastClickedAt = null,
                metadata = metadata
            )

            every {
                shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
            } returns shortUrlDocument

            val result = totalStatsService.resolveTotalStats(totalStatsVo)

            Then("TotalStats로 변환해야 한다") {
                result shouldBe TotalStats(
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
                    lastClickedAt = totalStatsVo.lastClickedAt,
                    metadata = metadata
                )
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
                }
            }
        }

        When("shortKey에 해당하는 document가 없는 경우") {
            every {
                shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
            } returns null

            val result = totalStatsService.resolveTotalStats(totalStatsVo)

            Then("null을 반환해야 한다") {
                result shouldBe null
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
                }
            }
        }

        When("빈 리스트가 있는 경우") {
            val emptyTotalStatsVo = TotalStatsVo(
                shortKey = shortKey,
                totalClicks = 0L,
                byDate = emptyList(),
                byDevice = emptyList(),
                byReferrer = emptyList(),
                lastClickedAt = null
            )

            val shortUrlDocument = ShortUrlTotalStatsDocument(
                shortKey = shortKey,
                totalClicks = 0L,
                byDate = mutableMapOf(),
                byDevice = mutableMapOf(),
                byReferrer = mutableMapOf(),
                lastClickedAt = null,
                metadata = metadata
            )

            every {
                shortUrlTotalStatsRepository.findByIdOrNull(shortKey)
            } returns shortUrlDocument

            val result = totalStatsService.resolveTotalStats(emptyTotalStatsVo)

            Then("빈 리스트로 TotalStats를 반환해야 한다") {
                result shouldBe TotalStats(
                    shortKey = shortKey,
                    totalClicks = 0L,
                    byDate = emptyList(),
                    byDevice = emptyList(),
                    byReferrer = emptyList(),
                    lastClickedAt = null,
                    metadata = metadata
                )
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
                totalStatsRepository.save(any<TotalStats>())
            } returns Unit

            totalStatsService.save(totalStats)

            Then("Repository의 save를 호출해야 한다") {
                verify(exactly = 1) {
                    totalStatsRepository.save(totalStats)
                }
            }
        }
    }
})

