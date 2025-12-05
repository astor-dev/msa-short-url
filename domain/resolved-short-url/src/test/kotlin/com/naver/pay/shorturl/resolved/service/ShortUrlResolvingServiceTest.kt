package com.naver.pay.shorturl.resolved.service

import com.naver.pay.shorturl.ShortUrl
import com.naver.pay.shorturl.ShortUrlCacheableService
import com.naver.pay.shorturl.resolved.CacheNames
import com.naver.pay.shorturl.resolved.ClickSummary
import com.naver.pay.shorturl.resolved.ResolvedShortUrlCacheService
import com.naver.pay.shorturl.resolved.ShortUrlResolvingService
import com.naver.pay.shorturl.resolved.ShortUrlSummaryService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class ShortUrlResolvingServiceTest : BehaviorSpec({
    val shortUrlSummaryService = mockk<ShortUrlSummaryService>()
    val resolvedShortUrlCacheService = mockk<ResolvedShortUrlCacheService>()
    val shortUrlCacheableService = mockk<ShortUrlCacheableService>()
    val shortUrlResolvingService = ShortUrlResolvingService(
        shortUrlSummaryService,
        resolvedShortUrlCacheService,
        shortUrlCacheableService
    )

    val shortKey = "testKey"
    val originalUrl = "https://naver.com"
    val now = Instant.now()
    val shortUrl = ShortUrl.of(
        id = 1L,
        shortKey = shortKey,
        baseUrl = "http://localhost",
        originalUrl = originalUrl,
        createdAt = now,
        expiresAt = now.plusSeconds(3600)
    )

    val totalClicksCacheKey = "${CacheNames.SHORT_URL_TOTAL_CLICKS}::$shortKey"
    val lastClickedAtCacheKey = "${CacheNames.SHORT_URL_LAST_CLICKED_AT}::$shortKey"

    Given("단축 URL 해석(resolve) 요청이 주어졌을 때") {
        When("유효한 단축 키이고 캐시에 클릭 정보가 존재하면") {
            val clickSummaryFromCache = ClickSummary(10, Instant.now())
            every { shortUrlCacheableService.findShortUrlByShortKey(shortKey) } returns shortUrl
            every { resolvedShortUrlCacheService.findClickSummary(totalClicksCacheKey, lastClickedAtCacheKey) } returns clickSummaryFromCache

            val result = shortUrlResolvingService.resolveShortUrl(shortKey)

            Then("캐시의 정보와 원본 URL 정보를 조합하여 ResolvedShortUrl을 반환한다") {
                result shouldNotBe null
                result!!.shortKey shouldBe shortKey
                result.originalUrl shouldBe originalUrl
                result.clickSummary shouldBe clickSummaryFromCache
                verify(exactly = 0) { shortUrlSummaryService.findClickSummaryFromPersistence(any()) }
            }
        }

        When("유효한 단축 키이고 캐시에 클릭 정보가 없으면") {
            val clickSummaryFromDb = ClickSummary(5, Instant.now())
            every { shortUrlCacheableService.findShortUrlByShortKey(shortKey) } returns shortUrl
            every { resolvedShortUrlCacheService.findClickSummary(totalClicksCacheKey, lastClickedAtCacheKey) } returns null
            every { shortUrlSummaryService.findClickSummaryFromPersistence(shortKey) } returns clickSummaryFromDb

            val result = shortUrlResolvingService.resolveShortUrl(shortKey)

            Then("DB의 정보와 원본 URL 정보를 조합하여 ResolvedShortUrl을 반환한다") {
                result shouldNotBe null
                result!!.shortKey shouldBe shortKey
                result.originalUrl shouldBe originalUrl
                result.clickSummary shouldBe clickSummaryFromDb
                verify(exactly = 1) { shortUrlSummaryService.findClickSummaryFromPersistence(shortKey) }
            }
        }

        When("존재하지 않는 단축 키이면") {
            every { shortUrlCacheableService.findShortUrlByShortKey(shortKey) } returns null

            val result = shortUrlResolvingService.resolveShortUrl(shortKey)

            Then("null을 반환한다") {
                result shouldBe null
            }
        }
    }
})
