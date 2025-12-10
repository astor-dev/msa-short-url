package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.mongodb.DailyTopByDeviceUrlsDocument
import com.naver.pay.shorturl.stats.mongodb.DailyTopByDeviceUrlsRepository
import com.naver.pay.shorturl.stats.mongodb.DailyTopDevicesDocument
import com.naver.pay.shorturl.stats.mongodb.DailyTopDevicesRepository
import com.naver.pay.shorturl.stats.mongodb.DailyTopReferrersDocument
import com.naver.pay.shorturl.stats.mongodb.DailyTopReferrersRepository
import com.naver.pay.shorturl.stats.mongodb.DailyTopUrlsDocument
import com.naver.pay.shorturl.stats.mongodb.DailyTopUrlsRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import java.time.Instant
import java.time.LocalDate
import java.util.Collections

class DailyTopStatsRepositoryTest : BehaviorSpec({
    val dailyTopUrlsRepository = mockk<DailyTopUrlsRepository>()
    val dailyTopReferrersRepository = mockk<DailyTopReferrersRepository>()
    val dailyTopDevicesRepository = mockk<DailyTopDevicesRepository>()
    val dailyTopByDeviceUrlsRepository = mockk<DailyTopByDeviceUrlsRepository>()
    val redisTemplate = mockk<RedisTemplate<String, String>>()
    val zSetOperations = mockk<ZSetOperations<String, String>>()

    val dailyTopStatsRepository = DailyTopStatsRepository(
        dailyTopUrlsRepository = dailyTopUrlsRepository,
        dailyTopReferrersRepository = dailyTopReferrersRepository,
        dailyTopDevicesRepository = dailyTopDevicesRepository,
        dailyTopByDeviceUrlsRepository = dailyTopByDeviceUrlsRepository,
        redisTemplate = redisTemplate
    )

    beforeTest {
        every { redisTemplate.opsForZSet() } returns zSetOperations
    }

    afterTest {
        clearAllMocks()
    }

    Given("findOne 메소드가 주어졌을 때") {
        val date = LocalDate.of(2024, 1, 1)
        val dateString = "2024-01-01"
        val limit = 10L
        val pageable = PageRequest.of(0, limit.toInt(), Sort.by(Sort.Direction.ASC, "rank"))

        When("데이터가 존재하는 경우") {
            val topUrlDocument = DailyTopUrlsDocument(
                id = "2024-01-01_key1",
                date = dateString,
                rank = 1,
                shortKey = "key1",
                totalClicks = 100L,
                shortUrl = "https://short.naver.com/key1",
                originalUrl = "https://naver.com",
                lastUpdatedAt = Instant.now()
            )
            val topReferrerDocument = DailyTopReferrersDocument(
                id = "2024-01-01_referrer1",
                date = dateString,
                rank = 1,
                referrer = "referrer1",
                totalClicks = 50L,
                lastUpdatedAt = Instant.now()
            )
            val topDeviceDocument = DailyTopDevicesDocument(
                id = "2024-01-01_mobile",
                date = dateString,
                deviceType = "mobile",
                totalClicks = 80L,
                lastUpdatedAt = Instant.now()
            )
            val topByDeviceUrlDocument = DailyTopByDeviceUrlsDocument(
                id = "2024-01-01_mobile_key1",
                date = dateString,
                deviceType = "mobile",
                rank = 1,
                shortKey = "key1",
                shortUrl = "https://short.naver.com/key1",
                originalUrl = "https://naver.com",
                clicksFromThisDevice = 80L,
                lastUpdatedAt = Instant.now()
            )

            every {
                dailyTopUrlsRepository.findByDateOrderByRankAsc(dateString, pageable)
            } returns listOf(topUrlDocument)
            every {
                dailyTopReferrersRepository.findByDateOrderByRankAsc(dateString, pageable)
            } returns listOf(topReferrerDocument)
            every {
                dailyTopDevicesRepository.findByDateOrderByTotalClicksDesc(dateString)
            } returns listOf(topDeviceDocument)
            every {
                dailyTopByDeviceUrlsRepository.findByDateAndDeviceTypeOrderByRankAsc(
                    dateString,
                    "mobile",
                    pageable
                )
            } returns listOf(topByDeviceUrlDocument)

            val result = dailyTopStatsRepository.findOne(date, limit)

            Then("DailyTopStats를 반환해야 한다") {
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
                verify(exactly = 1) {
                    dailyTopUrlsRepository.findByDateOrderByRankAsc(dateString, pageable)
                }
                verify(exactly = 1) {
                    dailyTopReferrersRepository.findByDateOrderByRankAsc(dateString, pageable)
                }
                verify(exactly = 1) {
                    dailyTopDevicesRepository.findByDateOrderByTotalClicksDesc(dateString)
                }
                verify(exactly = 1) {
                    dailyTopByDeviceUrlsRepository.findByDateAndDeviceTypeOrderByRankAsc(
                        dateString,
                        "mobile",
                        pageable
                    )
                }
            }
        }

        When("topUrlsDocuments가 비어있는 경우") {
            every {
                dailyTopUrlsRepository.findByDateOrderByRankAsc(dateString, pageable)
            } returns emptyList()

            val result = dailyTopStatsRepository.findOne(date, limit)

            Then("null을 반환해야 한다") {
                result shouldBe null
                verify(exactly = 1) {
                    dailyTopUrlsRepository.findByDateOrderByRankAsc(dateString, pageable)
                }
                verify(exactly = 0) {
                    dailyTopReferrersRepository.findByDateOrderByRankAsc(any(), any())
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
                dailyTopUrlsRepository.saveAll(any<List<DailyTopUrlsDocument>>())
            } returns emptyList()
            every {
                dailyTopReferrersRepository.saveAll(any<List<DailyTopReferrersDocument>>())
            } returns emptyList()
            every {
                dailyTopDevicesRepository.saveAll(any<List<DailyTopDevicesDocument>>())
            } returns emptyList()
            every {
                dailyTopByDeviceUrlsRepository.saveAll(any<List<DailyTopByDeviceUrlsDocument>>())
            } returns emptyList()

            dailyTopStatsRepository.save(dailyTopStats)

            Then("모든 Repository에 저장해야 한다") {
                verify(exactly = 1) {
                    dailyTopUrlsRepository.saveAll(any<List<DailyTopUrlsDocument>>())
                }
                verify(exactly = 1) {
                    dailyTopReferrersRepository.saveAll(any<List<DailyTopReferrersDocument>>())
                }
                verify(exactly = 1) {
                    dailyTopDevicesRepository.saveAll(any<List<DailyTopDevicesDocument>>())
                }
                verify(exactly = 1) {
                    dailyTopByDeviceUrlsRepository.saveAll(any<List<DailyTopByDeviceUrlsDocument>>())
                }
            }
        }
    }

    Given("recordClickAtomically 메소드가 주어졌을 때") {
        val date = LocalDate.of(2024, 1, 1)
        val dateKey = "2024-01-01"
        val shortKey = "testKey"
        val referrer = "testReferrer"
        val device = "mobile"

        When("정상적으로 실행하는 경우") {1
            every {
                redisTemplate.execute<Any?>(any(), any(), *anyVararg<Any>())
            } returns Unit

            dailyTopStatsRepository.recordClickToCache(date, shortKey, referrer, device)

            Then("Redis 스크립트를 실행해야 한다") {
                verify(exactly = 1) {
                    redisTemplate.execute(any(), any(), *anyVararg<Any>())
                }
            }
        }
    }

    Given("findDailyStatsInCache 메소드가 주어졌을 때") {
        val dateKey = "2024-01-01"
        val limit = 10L
        val urlKey = "${CacheNames.DAILY_TOP_URLS}::{$dateKey}"
        val referrerKey = "${CacheNames.DAILY_TOP_REFERRERS}::{$dateKey}"
        val deviceParentKey = "${CacheNames.DAILY_TOP_DEVICES}::{$dateKey}"
        val deviceChildKey = "${CacheNames.DAILY_TOP_DEVICES}::{$dateKey}::${CacheNames.INFIX_DAILY_TOP_URLS}::{mobile}"

        When("캐시에 데이터가 존재하는 경우") {
            val urlTuple = mockk<ZSetOperations.TypedTuple<String>> {
                every { value } returns "key1"
                every { score } returns 100.0
            }
            val referrerTuple = mockk<ZSetOperations.TypedTuple<String>> {
                every { value } returns "referrer1"
                every { score } returns 50.0
            }
            val deviceParentTuple = mockk<ZSetOperations.TypedTuple<String>> {
                every { value } returns "mobile"
                every { score } returns 80.0
            }
            val deviceChildTuple = mockk<ZSetOperations.TypedTuple<String>> {
                every { value } returns "key1"
                every { score } returns 80.0
            }

            every { redisTemplate.hasKey(urlKey) } returns true
            every {
                zSetOperations.reverseRangeWithScores(urlKey, 0, limit - 1)
            } returns setOf(urlTuple)
            every {
                zSetOperations.reverseRangeWithScores(referrerKey, 0, limit - 1)
            } returns setOf(referrerTuple)
            every {
                zSetOperations.reverseRangeWithScores(deviceParentKey, 0, -1)
            } returns setOf(deviceParentTuple)
            every {
                zSetOperations.reverseRangeWithScores(deviceChildKey, 0, limit - 1)
            } returns setOf(deviceChildTuple)

            val result = dailyTopStatsRepository.findDailyStatsInCache(dateKey, limit)

            Then("DailyStatsVo를 반환해야 한다") {
                result shouldBe DailyStatsVo(
                    dateKey = dateKey,
                    topUrls = listOf(
                        KeyCountVo(key = "key1", count = 100L)
                    ),
                    topReferrers = listOf(
                        KeyCountVo(key = "referrer1", count = 50L)
                    ),
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
                verify(exactly = 1) { redisTemplate.hasKey(urlKey) }
                verify(exactly = 1) {
                    zSetOperations.reverseRangeWithScores(urlKey, 0, limit - 1)
                }
                verify(exactly = 1) {
                    zSetOperations.reverseRangeWithScores(referrerKey, 0, limit - 1)
                }
                verify(exactly = 1) {
                    zSetOperations.reverseRangeWithScores(deviceParentKey, 0, -1)
                }
                verify(exactly = 1) {
                    zSetOperations.reverseRangeWithScores(deviceChildKey, 0, limit - 1)
                }
            }
        }

        When("캐시에 데이터가 존재하지 않는 경우") {
            every { redisTemplate.hasKey(urlKey) } returns false

            val result = dailyTopStatsRepository.findDailyStatsInCache(dateKey, limit)

            Then("null을 반환해야 한다") {
                result shouldBe null
                verify(exactly = 1) { redisTemplate.hasKey(urlKey) }
                verify(exactly = 0) {
                    zSetOperations.reverseRangeWithScores(any(), any(), any())
                }
            }
        }

        When("limit이 -1인 경우 (모든 데이터 조회)") {
            val deviceParentTuple = mockk<ZSetOperations.TypedTuple<String>> {
                every { value } returns "mobile"
                every { score } returns 80.0
            }

            every { redisTemplate.hasKey(urlKey) } returns true
            every {
                zSetOperations.reverseRangeWithScores(urlKey, 0, -1)
            } returns Collections.emptySet()
            every {
                zSetOperations.reverseRangeWithScores(referrerKey, 0, -1)
            } returns Collections.emptySet()
            every {
                zSetOperations.reverseRangeWithScores(deviceParentKey, 0, -1)
            } returns setOf(deviceParentTuple)
            every {
                zSetOperations.reverseRangeWithScores(deviceChildKey, 0, -1)
            } returns Collections.emptySet()

            val result = dailyTopStatsRepository.findDailyStatsInCache(dateKey, -1)

            Then("모든 데이터를 조회해야 한다") {
                result shouldBe DailyStatsVo(
                    dateKey = dateKey,
                    topUrls = emptyList(),
                    topReferrers = emptyList(),
                    topByDevice = listOf(
                        DeviceStatsVo(
                            deviceType = "mobile",
                            totalCount = 80L,
                            topUrls = emptyList()
                        )
                    )
                )
                verify(exactly = 1) {
                    zSetOperations.reverseRangeWithScores(urlKey, 0, -1)
                }
                verify(exactly = 1) {
                    zSetOperations.reverseRangeWithScores(referrerKey, 0, -1)
                }
                verify(exactly = 1) {
                    zSetOperations.reverseRangeWithScores(deviceParentKey, 0, -1)
                }
            }
        }

        When("tuple의 value가 null인 경우") {
            val urlTuple = mockk<ZSetOperations.TypedTuple<String>> {
                every { value } returns null
                every { score } returns 100.0
            }

            every { redisTemplate.hasKey(urlKey) } returns true
            every {
                zSetOperations.reverseRangeWithScores(urlKey, 0, limit - 1)
            } returns setOf(urlTuple)
            every {
                zSetOperations.reverseRangeWithScores(referrerKey, 0, limit - 1)
            } returns Collections.emptySet()
            every {
                zSetOperations.reverseRangeWithScores(deviceParentKey, 0, -1)
            } returns Collections.emptySet()

            val result = dailyTopStatsRepository.findDailyStatsInCache(dateKey, limit)

            Then("null value는 필터링되어야 한다") {
                result shouldBe DailyStatsVo(
                    dateKey = dateKey,
                    topUrls = emptyList(),
                    topReferrers = emptyList(),
                    topByDevice = emptyList()
                )
            }
        }

        When("tuple의 score가 null인 경우") {
            val urlTuple = mockk<ZSetOperations.TypedTuple<String>> {
                every { value } returns "key1"
                every { score } returns null
            }

            every { redisTemplate.hasKey(urlKey) } returns true
            every {
                zSetOperations.reverseRangeWithScores(urlKey, 0, limit - 1)
            } returns setOf(urlTuple)
            every {
                zSetOperations.reverseRangeWithScores(referrerKey, 0, limit - 1)
            } returns Collections.emptySet()
            every {
                zSetOperations.reverseRangeWithScores(deviceParentKey, 0, -1)
            } returns Collections.emptySet()

            val result = dailyTopStatsRepository.findDailyStatsInCache(dateKey, limit)

            Then("count는 0으로 설정되어야 한다") {
                result shouldBe DailyStatsVo(
                    dateKey = dateKey,
                    topUrls = listOf(KeyCountVo(key = "key1", count = 0L)),
                    topReferrers = emptyList(),
                    topByDevice = emptyList()
                )
            }
        }
    }
})

