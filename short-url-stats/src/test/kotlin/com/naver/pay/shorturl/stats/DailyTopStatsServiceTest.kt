package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsDocument
import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate

class DailyTopStatsServiceTest : BehaviorSpec({
    val dailyTopStatsRepository = mockk<DailyTopStatsRepository>()
    val shortUrlTotalStatsRepository = mockk<ShortUrlTotalStatsRepository>()
    val dailyTopStatsService = DailyTopStatsService(
        dailyTopStatsRepository = dailyTopStatsRepository,
        shortUrlTotalStatsRepository = shortUrlTotalStatsRepository
    )

    afterTest {
        clearAllMocks()
    }

    Given("getOne 메소드가 주어졌을 때") {
        val date = LocalDate.of(2024, 1, 1)
        val dateString = "2024-01-01"
        val limit = 10L

        When("캐시에 데이터가 존재하는 경우") {
            val dailyStatsVo = DailyStatsVo(
                dateKey = dateString,
                topUrls = listOf(
                    KeyCountVo(key = "key1", count = 100L)
                ),
                topReferrers = listOf(
                    KeyCountVo(key = "referrer1", count = 50L)
                ),
                topByDevice = emptyList()
            )

            val shortUrlDocument = ShortUrlTotalStatsDocument(
                shortKey = "key1",
                totalClicks = 100L,
                byDate = mutableMapOf(),
                byDevice = mutableMapOf(),
                byReferrer = mutableMapOf(),
                lastClickedAt = null,
                metadata = ShortUrlMetadata(
                    shortUrl = "https://short.naver.com/key1",
                    originalUrl = "https://naver.com",
                    shortUrlCreatedAt = Instant.now(),
                    shortUrlExpiredAt = Instant.now().plusSeconds(3600)
                )
            )

            every {
                dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
            } returns dailyStatsVo
            every {
                shortUrlTotalStatsRepository.findAllById(setOf("key1"))
            } returns listOf(shortUrlDocument)

            val result = dailyTopStatsService.getOne(date, limit)

            Then("캐시 데이터를 resolve하여 DailyTopStats를 반환해야 한다") {
                result shouldBe DailyTopStats(
                    date = dateString,
                    topUrls = listOf(
                        TopUrlInfo(
                            rank = 1,
                            shortKey = "key1",
                            shortUrl = "https://short.naver.com/key1",
                            originalUrl = "https://naver.com",
                            totalClicks = 100L
                        )
                    ),
                    topReferrers = listOf(
                        TopReferrerInfo(
                            rank = 1,
                            referrer = "referrer1",
                            totalClicks = 50L
                        )
                    ),
                    topByDevice = emptyList()
                )
                verify(exactly = 1) {
                    dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
                }
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findAllById(setOf("key1"))
                }
                verify(exactly = 0) {
                    dailyTopStatsRepository.findOne(any(), any())
                }
            }
        }

        When("캐시에 데이터가 없고 영속화된 통계가 존재하는 경우") {
            val statsFromPersistence = DailyTopStats(
                date = dateString,
                topUrls = listOf(
                    TopUrlInfo(
                        rank = 1,
                        shortKey = "key1",
                        shortUrl = "https://short.naver.com/key1",
                        originalUrl = "https://naver.com",
                        totalClicks = 100L
                    )
                ),
                topReferrers = emptyList(),
                topByDevice = emptyList()
            )

            every {
                dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
            } returns null
            every {
                dailyTopStatsRepository.findOne(date, limit)
            } returns statsFromPersistence

            val result = dailyTopStatsService.getOne(date, limit)

            Then("영속화된 통계를 반환해야 한다") {
                result shouldBe statsFromPersistence
                verify(exactly = 1) {
                    dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
                }
                verify(exactly = 1) {
                    dailyTopStatsRepository.findOne(date, limit)
                }
                verify(exactly = 0) {
                    shortUrlTotalStatsRepository.findAllById(any())
                }
            }
        }

        When("캐시에 데이터가 없고 영속화된 통계도 없는 경우") {
            every {
                dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
            } returns null
            every {
                dailyTopStatsRepository.findOne(date, limit)
            } returns null

            val result = dailyTopStatsService.getOne(date, limit)

            Then("빈 DailyTopStats를 반환해야 한다") {
                result shouldBe DailyTopStats(date = dateString)
                verify(exactly = 1) {
                    dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
                }
                verify(exactly = 1) {
                    dailyTopStatsRepository.findOne(date, limit)
                }
                verify(exactly = 0) {
                    shortUrlTotalStatsRepository.findAllById(any())
                }
            }
        }

        When("캐시에 데이터가 없고 영속화된 통계도 없는 경우") {
            every {
                dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
            } returns null
            every {
                dailyTopStatsRepository.findOne(date, limit)
            } returns null

            val result = dailyTopStatsService.getOne(date, limit)

            Then("빈 DailyTopStats를 반환해야 한다") {
                result shouldBe DailyTopStats(date = dateString)
                verify(exactly = 1) {
                    dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
                }
                verify(exactly = 1) {
                    dailyTopStatsRepository.findOne(date, limit)
                }
                verify(exactly = 0) {
                    shortUrlTotalStatsRepository.findAllById(any())
                }
            }
        }

        When("캐시에 topByDevice 데이터가 있는 경우") {
            val dailyStatsVo = DailyStatsVo(
                dateKey = dateString,
                topUrls = emptyList(),
                topReferrers = emptyList(),
                topByDevice = listOf(
                    DeviceStatsVo(
                        deviceType = "mobile",
                        totalCount = 80L,
                        topUrls = listOf(
                            KeyCountVo(key = "key1", count = 80L)
                        )
                    )
                )
            )

            val shortUrlDocument = ShortUrlTotalStatsDocument(
                shortKey = "key1",
                totalClicks = 80L,
                byDate = mutableMapOf(),
                byDevice = mutableMapOf(),
                byReferrer = mutableMapOf(),
                lastClickedAt = null,
                metadata = ShortUrlMetadata(
                    shortUrl = "https://short.naver.com/key1",
                    originalUrl = "https://naver.com",
                    shortUrlCreatedAt = Instant.now(),
                    shortUrlExpiredAt = Instant.now().plusSeconds(3600)
                )
            )

            every {
                dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
            } returns dailyStatsVo
            every {
                shortUrlTotalStatsRepository.findAllById(setOf("key1"))
            } returns listOf(shortUrlDocument)

            val result = dailyTopStatsService.getOne(date, limit)

            Then("topByDevice 데이터도 resolve하여 반환해야 한다") {
                result.topByDevice shouldBe listOf(
                    TopByDeviceInfo(
                        deviceType = "mobile",
                        totalClicks = 80L,
                        topUrls = listOf(
                            TopUrlInfo(
                                rank = 1,
                                shortKey = "key1",
                                shortUrl = "https://short.naver.com/key1",
                                originalUrl = "https://naver.com",
                                totalClicks = 80L
                            )
                        )
                    )
                )
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findAllById(setOf("key1"))
                }
            }
        }
    }

    Given("recordClickAtomically 메소드가 주어졌을 때") {
        val date = LocalDate.of(2024, 1, 1)
        val shortKey = "testKey"
        val referrer = "testReferrer"
        val device = "mobile"

        When("정상적으로 실행하는 경우") {
            every {
                dailyTopStatsRepository.recordClickToCache(date, shortKey, referrer, device)
            } returns Unit

            dailyTopStatsService.captureClick(date, shortKey, referrer, device)

            Then("Repository의 recordClickAtomically를 호출해야 한다") {
                verify(exactly = 1) {
                    dailyTopStatsRepository.recordClickToCache(date, shortKey, referrer, device)
                }
            }
        }
    }

    Given("resolveTotalStats 메소드가 주어졌을 때") {
        val dateString = "2024-01-01"
        val dailyStatsVo = DailyStatsVo(
            dateKey = dateString,
            topUrls = listOf(
                KeyCountVo(key = "key1", count = 100L),
                KeyCountVo(key = "key2", count = 50L)
            ),
            topReferrers = listOf(
                KeyCountVo(key = "referrer1", count = 80L),
                KeyCountVo(key = "referrer2", count = 30L)
            ),
            topByDevice = listOf(
                DeviceStatsVo(
                    deviceType = "mobile",
                    totalCount = 90L,
                    topUrls = listOf(
                        KeyCountVo(key = "key1", count = 90L)
                    )
                ),
                DeviceStatsVo(
                    deviceType = "desktop",
                    totalCount = 60L,
                    topUrls = listOf(
                        KeyCountVo(key = "key2", count = 60L)
                    )
                )
            )
        )

        When("모든 shortKey에 해당하는 document가 존재하는 경우") {
            val shortUrlDocument1 = ShortUrlTotalStatsDocument(
                shortKey = "key1",
                totalClicks = 100L,
                byDate = mutableMapOf(),
                byDevice = mutableMapOf(),
                byReferrer = mutableMapOf(),
                lastClickedAt = null,
                metadata = ShortUrlMetadata(
                    shortUrl = "https://short.naver.com/key1",
                    originalUrl = "https://naver.com",
                    shortUrlCreatedAt = Instant.now(),
                    shortUrlExpiredAt = Instant.now().plusSeconds(3600)
                )
            )
            val shortUrlDocument2 = ShortUrlTotalStatsDocument(
                shortKey = "key2",
                totalClicks = 50L,
                byDate = mutableMapOf(),
                byDevice = mutableMapOf(),
                byReferrer = mutableMapOf(),
                lastClickedAt = null,
                metadata = ShortUrlMetadata(
                    shortUrl = "https://short.naver.com/key2",
                    originalUrl = "https://google.com",
                    shortUrlCreatedAt = Instant.now(),
                    shortUrlExpiredAt = Instant.now().plusSeconds(3600)
                )
            )

            every {
                shortUrlTotalStatsRepository.findAllById(setOf("key1", "key2"))
            } returns listOf(shortUrlDocument1, shortUrlDocument2)

            val result = dailyTopStatsService.resolveTotalStats(dailyStatsVo)

            Then("DailyTopStats로 변환해야 한다") {
                result shouldBe DailyTopStats(
                    date = dateString,
                    topUrls = listOf(
                        TopUrlInfo(
                            rank = 1,
                            shortKey = "key1",
                            shortUrl = "https://short.naver.com/key1",
                            originalUrl = "https://naver.com",
                            totalClicks = 100L
                        ),
                        TopUrlInfo(
                            rank = 2,
                            shortKey = "key2",
                            shortUrl = "https://short.naver.com/key2",
                            originalUrl = "https://google.com",
                            totalClicks = 50L
                        )
                    ),
                    topReferrers = listOf(
                        TopReferrerInfo(
                            rank = 1,
                            referrer = "referrer1",
                            totalClicks = 80L
                        ),
                        TopReferrerInfo(
                            rank = 2,
                            referrer = "referrer2",
                            totalClicks = 30L
                        )
                    ),
                    topByDevice = listOf(
                        TopByDeviceInfo(
                            deviceType = "mobile",
                            totalClicks = 90L,
                            topUrls = listOf(
                                TopUrlInfo(
                                    rank = 1,
                                    shortKey = "key1",
                                    shortUrl = "https://short.naver.com/key1",
                                    originalUrl = "https://naver.com",
                                    totalClicks = 90L
                                )
                            )
                        ),
                        TopByDeviceInfo(
                            deviceType = "desktop",
                            totalClicks = 60L,
                            topUrls = listOf(
                                TopUrlInfo(
                                    rank = 1,
                                    shortKey = "key2",
                                    shortUrl = "https://short.naver.com/key2",
                                    originalUrl = "https://google.com",
                                    totalClicks = 60L
                                )
                            )
                        )
                    )
                )
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findAllById(setOf("key1", "key2"))
                }
            }
        }

        When("shortKey에 해당하는 document가 없는 경우") {
            every {
                shortUrlTotalStatsRepository.findAllById(setOf("key1", "key2"))
            } returns emptyList()

            Then("NoSuchElementException을 던져야 한다") {
                shouldThrow<NoSuchElementException> {
                    dailyTopStatsService.resolveTotalStats(dailyStatsVo)
                }
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findAllById(setOf("key1", "key2"))
                }
            }
        }

        When("일부 shortKey에 해당하는 document만 존재하는 경우") {
            val shortUrlDocument1 = ShortUrlTotalStatsDocument(
                shortKey = "key1",
                totalClicks = 100L,
                byDate = mutableMapOf(),
                byDevice = mutableMapOf(),
                byReferrer = mutableMapOf(),
                lastClickedAt = null,
                metadata = ShortUrlMetadata(
                    shortUrl = "https://short.naver.com/key1",
                    originalUrl = "https://naver.com",
                    shortUrlCreatedAt = Instant.now(),
                    shortUrlExpiredAt = Instant.now().plusSeconds(3600)
                )
            )

            every {
                shortUrlTotalStatsRepository.findAllById(setOf("key1", "key2"))
            } returns listOf(shortUrlDocument1)

            Then("NoSuchElementException을 던져야 한다") {
                shouldThrow<NoSuchElementException> {
                    dailyTopStatsService.resolveTotalStats(dailyStatsVo)
                }
                verify(exactly = 1) {
                    shortUrlTotalStatsRepository.findAllById(setOf("key1", "key2"))
                }
            }
        }
    }

    Given("save 메소드가 주어졌을 때") {
        val dateString = "2024-01-01"
        val dailyTopStats = DailyTopStats(
            date = dateString,
            topUrls = listOf(
                TopUrlInfo(
                    rank = 1,
                    shortKey = "key1",
                    shortUrl = "https://short.naver.com/key1",
                    originalUrl = "https://naver.com",
                    totalClicks = 100L
                )
            ),
            topReferrers = listOf(
                TopReferrerInfo(
                    rank = 1,
                    referrer = "referrer1",
                    totalClicks = 50L
                )
            ),
            topByDevice = listOf(
                TopByDeviceInfo(
                    deviceType = "mobile",
                    totalClicks = 80L,
                    topUrls = listOf(
                        TopUrlInfo(
                            rank = 1,
                            shortKey = "key1",
                            shortUrl = "https://short.naver.com/key1",
                            originalUrl = "https://naver.com",
                            totalClicks = 80L
                        )
                    )
                )
            )
        )

        When("정상적으로 저장하는 경우") {
            every {
                dailyTopStatsRepository.save(any<DailyTopStats>())
            } returns Unit

            dailyTopStatsService.save(dailyTopStats)

            Then("Repository의 save를 호출해야 한다") {
                verify(exactly = 1) {
                    dailyTopStatsRepository.save(dailyTopStats)
                }
            }
        }
    }
})

