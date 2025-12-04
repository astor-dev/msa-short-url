package com.naver.pay.service

import com.naver.pay.controller.v1.ClickSummaryResponseDto
import com.naver.pay.controller.v1.ShortUrlStateResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsByDateResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsByDeviceTypeResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsByReferrerResponseDto
import com.naver.pay.controller.v1.ShortUrlStatisticsResponseDto
import com.naver.pay.shorturl.resolved.ClickSummary
import com.naver.pay.shorturl.resolved.ResolvedShortUrl
import com.naver.pay.shorturl.resolved.ResolvedShortUrlCachableService
import com.naver.pay.shorturl.stats.ShortUrlDailyTopStatsService
import com.naver.pay.shorturl.stats.ShortUrlMetadata
import com.naver.pay.shorturl.stats.ShortUrlStatsByDate
import com.naver.pay.shorturl.stats.ShortUrlStatsByDevice
import com.naver.pay.shorturl.stats.ShortUrlStatsByReferrer
import com.naver.pay.shorturl.stats.ShortUrlTotalStats
import com.naver.pay.shorturl.stats.ShortUrlTotalStatsService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.NoSuchElementException


class ShortUrlStatsServiceTest : BehaviorSpec({
    val resolvedShortUrlCachableService = mockk<ResolvedShortUrlCachableService>()
    val shortUrlTotalStatsService = mockk<ShortUrlTotalStatsService>()
    val shortUrlDailyTopStatsService = mockk<ShortUrlDailyTopStatsService>()
    val shortUrlStatsService = ShortUrlStatsService(resolvedShortUrlCachableService, shortUrlTotalStatsService, shortUrlDailyTopStatsService)

    afterTest {
        clearAllMocks()
    }

    Given("ShortUrlStatsService가 주어졌을 때") {
        val shortKey = "testKey"
        val now = Instant.now()

        When("findShortUrlStateOrThrow 호출 시") {
            And("존재하는 shortKey가 주어지면") {
                val resolvedShortUrl = ResolvedShortUrl(
                    shortKey = shortKey,
                    shortUrl = "http://localhost/$shortKey",
                    originalUrl = "https://naver.com",
                    createdAt = now,
                    expiredAt = now.plusSeconds(3600),
                    clickSummary = ClickSummary(
                        totalClicks = 123L,
                        lastClickedAt = now.minusSeconds(60)
                    )
                )
                val expectedDto = ShortUrlStateResponseDto(
                    shortKey = shortKey,
                    shortUrl = "http://localhost/$shortKey",
                    originalUrl = "https://naver.com",
                    createdAt = now.truncatedTo(ChronoUnit.SECONDS).toString(),
                    expiresAt = now.plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS).toString(),
                    clickSummary = ClickSummaryResponseDto(
                        totalClicks = 123L,
                        lastClickedAt = now.minusSeconds(60).truncatedTo(ChronoUnit.SECONDS).toString()
                    )
                )

                every { resolvedShortUrlCachableService.findResolvedShortUrlOrThrow(shortKey) } returns resolvedShortUrl

                val result = shortUrlStatsService.findShortUrlStateOrThrow(shortKey)

                Then("ResolvedShortUrl의 DTO를 반환한다") {
                    result shouldBe expectedDto
                    verify(exactly = 1) { resolvedShortUrlCachableService.findResolvedShortUrlOrThrow(shortKey) }
                }
            }

            And("존재하지 않는 shortKey가 주어지면") {
                val errorMessage = "Short URL not found: $shortKey"
                every { resolvedShortUrlCachableService.findResolvedShortUrlOrThrow(shortKey) } throws NoSuchElementException(
                    errorMessage
                )

                val exception = runCatching {
                    shortUrlStatsService.findShortUrlStateOrThrow(shortKey)
                }.exceptionOrNull()

                Then("NoSuchElementException을 발생시킨다") {
                    exception.shouldBeInstanceOf<NoSuchElementException>()
                    exception.message shouldContain errorMessage
                    verify(exactly = 1) { resolvedShortUrlCachableService.findResolvedShortUrlOrThrow(shortKey) }
                }
            }
        }

        When("findShortUrlTotalStatsOrThrow 호출 시") {
            And("통계 정보가 존재하는 shortKey가 주어지면") {
                val totalStats = ShortUrlTotalStats(
                    shortKey = shortKey,
                    totalClicks = 1000L,
                    byDate = listOf(ShortUrlStatsByDate("2025-12-04", 500L)),
                    byDevice = listOf(ShortUrlStatsByDevice("PC", 800L)),
                    byReferrer = listOf(ShortUrlStatsByReferrer("google.com", 200L)),
                    lastClickedAt = now.minusSeconds(120),
                    metadata = ShortUrlMetadata(
                        shortUrl = "http://localhost/$shortKey",
                        originalUrl = "https://naver.com",
                        shortUrlCreatedAt = now,
                        shortUrlExpiredAt = now.plusSeconds(3600)
                    )
                )
                val expectedDto = ShortUrlStatisticsResponseDto(
                    shortKey = shortKey,
                    totalClicks = 1000L,
                    byDate = listOf(ShortUrlStatisticsByDateResponseDto("2025-12-04", 500L)),
                    byDevice = listOf(ShortUrlStatisticsByDeviceTypeResponseDto("PC", 800L)),
                    byReferrer = listOf(ShortUrlStatisticsByReferrerResponseDto("google.com", 200L))
                )

                every { shortUrlTotalStatsService.findOne(shortKey) } returns totalStats

                val result = shortUrlStatsService.findShortUrlTotalStatsOrThrow(shortKey)

                Then("ShortUrlTotalStats의 DTO를 반환한다") {
                    result shouldBe expectedDto
                    verify(exactly = 1) { shortUrlTotalStatsService.findOne(shortKey) }
                }
            }

            And("통계 정보가 존재하지 않는 shortKey가 주어지면") {
                every { shortUrlTotalStatsService.findOne(shortKey) } returns null

                val exception = runCatching {
                    shortUrlStatsService.findShortUrlTotalStatsOrThrow(shortKey)
                }.exceptionOrNull()

                Then("NoSuchElementException을 발생시킨다") {
                    exception.shouldBeInstanceOf<NoSuchElementException>()
                    exception.message shouldContain "Short URL not found: $shortKey"
                    verify(exactly = 1) { shortUrlTotalStatsService.findOne(shortKey) }
                }
            }
        }
    }
})

