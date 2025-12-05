package com.naver.pay.service

import com.naver.pay.controller.ClickSummaryResponseDto
import com.naver.pay.controller.ShortUrlStateResponseDto
import com.naver.pay.controller.ShortUrlStatisticsByDateResponseDto
import com.naver.pay.controller.ShortUrlStatisticsByDeviceTypeResponseDto
import com.naver.pay.controller.ShortUrlStatisticsByReferrerResponseDto
import com.naver.pay.controller.ShortUrlStatisticsResponseDto
import com.naver.pay.shorturl.resolved.ClickSummary
import com.naver.pay.shorturl.resolved.ResolvedShortUrl
import com.naver.pay.shorturl.resolved.ShortUrlResolvingService
import com.naver.pay.shorturl.stats.DailyTopStatsService
import com.naver.pay.shorturl.stats.ShortUrlMetadata
import com.naver.pay.shorturl.stats.ShortUrlStatsByDate
import com.naver.pay.shorturl.stats.ShortUrlStatsByDevice
import com.naver.pay.shorturl.stats.ShortUrlStatsByReferrer
import com.naver.pay.shorturl.stats.TotalStats
import com.naver.pay.shorturl.stats.TotalStatsService
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
//    val resolvedShortUrlCachableService = mockk<ResolvedShortUrlCachableService>()
    val shortUrlResolvingService = mockk<ShortUrlResolvingService>()
    val totalStatsService = mockk<TotalStatsService>()
    val shortUrlDailyTopStatsService = mockk<DailyTopStatsService>()
    val shortUrlStatsService = ShortUrlStatsService(shortUrlResolvingService, totalStatsService, shortUrlDailyTopStatsService)

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

                every { shortUrlResolvingService.resolveShortUrl(shortKey) } returns resolvedShortUrl

                val result = shortUrlStatsService.findShortUrlStateOrThrow(shortKey)

                Then("ResolvedShortUrl의 DTO를 반환한다") {
                    result shouldBe expectedDto
                    verify(exactly = 1) { shortUrlResolvingService.resolveShortUrl(shortKey) }
                }
            }

            And("존재하지 않는 shortKey가 주어지면") {
                val errorMessage = "Short URL not found: $shortKey"
                every { shortUrlResolvingService.resolveShortUrl(shortKey) } returns null

                val exception = runCatching {
                    shortUrlStatsService.findShortUrlStateOrThrow(shortKey)
                }.exceptionOrNull()

                Then("NoSuchElementException을 발생시킨다") {
                    exception.shouldBeInstanceOf<NoSuchElementException>()
                    exception.message shouldContain errorMessage
                    verify(exactly = 1) { shortUrlResolvingService.resolveShortUrl(shortKey) }
                }
            }
        }

        When("findShortUrlTotalStatsOrThrow 호출 시") {
            And("통계 정보가 존재하는 shortKey가 주어지면") {
                val totalStats = TotalStats(
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

                every { totalStatsService.findOne(shortKey) } returns totalStats

                val result = shortUrlStatsService.findShortUrlTotalStatsOrThrow(shortKey)

                Then("ShortUrlTotalStats의 DTO를 반환한다") {
                    result shouldBe expectedDto
                    verify(exactly = 1) { totalStatsService.findOne(shortKey) }
                }
            }

            And("통계 정보가 존재하지 않는 shortKey가 주어지면") {
                every { totalStatsService.findOne(shortKey) } returns null

                val exception = runCatching {
                    shortUrlStatsService.findShortUrlTotalStatsOrThrow(shortKey)
                }.exceptionOrNull()

                Then("NoSuchElementException을 발생시킨다") {
                    exception.shouldBeInstanceOf<NoSuchElementException>()
                    exception.message shouldContain "Short URL not found: $shortKey"
                    verify(exactly = 1) { totalStatsService.findOne(shortKey) }
                }
            }
        }
    }
})

