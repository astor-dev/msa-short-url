package com.naver.pay.shorturl.resolved

import com.naver.pay.shorturl.stats.TotalStatsService
import org.springframework.stereotype.Service

@Service
class ShortUrlSummaryService(
    private val totalStatsService: TotalStatsService,
    private val resolvedShortUrlRepository: ResolvedShortUrlRepository
) {

    /**
     * 단축 URL 클릭 시 클릭 수 및 최종 클릭 시각을 업데이트합니다.
     * 캐시에 존재하는 경우 빠른 업데이트 스크립트를 사용하고,
     * 캐시에 없는 경우 영속성으로부터 초기 값을 조회해 업데이트 스크립트를 사용합니다.
     * @param shortKey 단축 URL의 고유 키
     */
    fun incrementClickCount(shortKey: String) {
        val totalClicksCacheKey = "${CacheNames.SHORT_URL_TOTAL_CLICKS}::$shortKey"
        val lastClickedAtCacheKey = "${CacheNames.SHORT_URL_LAST_CLICKED_AT}::$shortKey"
        if (resolvedShortUrlRepository.hasCacheKey(totalClicksCacheKey)) {
            resolvedShortUrlRepository.recordClickAtomically(totalClicksCacheKey, lastClickedAtCacheKey)
            return
        }
        val clickSummary = findClickSummaryFromPersistence(shortKey)
        resolvedShortUrlRepository.upsertClick(shortKey, totalClicksCacheKey, lastClickedAtCacheKey, clickSummary)
    }

    fun findClickSummaryFromPersistence(shortKey: String): ClickSummary {
        return totalStatsService.findOne(shortKey)?.let {
            ClickSummary(
                totalClicks = it.totalClicks,
                lastClickedAt = it.lastClickedAt
            )
        } ?: ClickSummary()

    }
}