package com.naver.pay.shorturl.resolved.service

import com.naver.pay.shorturl.resolved.CacheNames
import com.naver.pay.shorturl.resolved.ClickSummary
import com.naver.pay.shorturl.resolved.ResolvedShortUrlCacheService
import com.naver.pay.shorturl.resolved.ShortUrlSummaryService
import com.naver.pay.shorturl.stats.ShortUrlTotalStats
import com.naver.pay.shorturl.stats.ShortUrlTotalStatsService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class ShortUrlSummaryServiceTest : BehaviorSpec({
    val shortUrlTotalStatsService = mockk<ShortUrlTotalStatsService>()
    val resolvedShortUrlCacheService = mockk<ResolvedShortUrlCacheService>()
    val shortUrlSummaryService = ShortUrlSummaryService(shortUrlTotalStatsService, resolvedShortUrlCacheService)

    val shortKey = "testKey"
    val totalClicksCacheKey = "${CacheNames.SHORT_URL_TOTAL_CLICKS}::$shortKey"
    val lastClickedAtCacheKey = "${CacheNames.SHORT_URL_LAST_CLICKED_AT}::$shortKey"

    afterTest {
        clearAllMocks()
    }

    Given("단축 URL 클릭 수 증가 요청이 주어졌을 때") {
        When("캐시에 이미 클릭 정보가 존재하면") {
            every { resolvedShortUrlCacheService.hasKey(totalClicksCacheKey) } returns true
            every { resolvedShortUrlCacheService.recordClickAtomically(any(), any()) } returns Unit

            shortUrlSummaryService.incrementClickCount(shortKey)

            Then("캐시의 클릭 수를 원자적으로 증가시킨다") {
                verify(exactly = 1) { resolvedShortUrlCacheService.recordClickAtomically(totalClicksCacheKey, lastClickedAtCacheKey) }
                verify(exactly = 0) { shortUrlTotalStatsService.findOne(any()) }
            }
        }

        When("캐시에 클릭 정보가 존재하지 않으면") {
            val now = Instant.now()
            val statsDomain = ShortUrlTotalStats(
                shortKey = shortKey,
                totalClicks = 10,
                lastClickedAt = now,
                metadata = mockk(),
                byDate = emptyList(),
                byDevice = emptyList(),
                byReferrer = emptyList(),
            )
            val expectedClickSummary = ClickSummary(totalClicks = 10, lastClickedAt = now)

            every { resolvedShortUrlCacheService.hasKey(totalClicksCacheKey) } returns false
            every { shortUrlTotalStatsService.findOne(shortKey) } returns statsDomain
            every { resolvedShortUrlCacheService.upsertClick(shortKey, totalClicksCacheKey, lastClickedAtCacheKey, expectedClickSummary) } returns Unit

            shortUrlSummaryService.incrementClickCount(shortKey)

            Then("영속성 계층에서 정보를 조회한 후 캐시를 업데이트한다") {
                verify(exactly = 1) { shortUrlTotalStatsService.findOne(shortKey) }
                verify(exactly = 1) { resolvedShortUrlCacheService.upsertClick(shortKey, totalClicksCacheKey, lastClickedAtCacheKey, expectedClickSummary) }
                verify(exactly = 0) { resolvedShortUrlCacheService.recordClickAtomically(any(), any()) }
            }
        }
    }

    Given("영속성 계층에서 클릭 요약 정보를 조회할 때") {
        When("통계 정보(Document)가 존재하면") {
            val now = Instant.now()
            val statsDomain = ShortUrlTotalStats(
                shortKey = shortKey,
                totalClicks = 10,
                lastClickedAt = now,
                metadata = mockk(),
                byDate = emptyList(),
                byDevice = emptyList(),
                byReferrer = emptyList(),
            )
            every { shortUrlTotalStatsService.findOne(shortKey) } returns statsDomain

            val result = shortUrlSummaryService.findClickSummaryFromPersistence(shortKey)

            Then("통계 정보를 ClickSummary 객체로 변환하여 반환한다") {
                result.totalClicks shouldBe statsDomain.totalClicks
                result.lastClickedAt shouldBe statsDomain.lastClickedAt
            }
        }

        When("통계 정보가 존재하지 않으면") {
            every { shortUrlTotalStatsService.findOne(shortKey) } returns null

            val result = shortUrlSummaryService.findClickSummaryFromPersistence(shortKey)

            Then("기본 값을 가진 ClickSummary 객체를 반환한다") {
                result.totalClicks shouldBe 0L
                result.lastClickedAt shouldBe null
            }
        }
    }
})
