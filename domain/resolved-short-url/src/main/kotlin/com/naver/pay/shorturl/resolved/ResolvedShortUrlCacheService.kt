package com.naver.pay.shorturl.resolved

interface ResolvedShortUrlCacheService {
    fun hasKey(key: String): Boolean
    fun recordClickAtomically(totalClicksCacheKey: String, lastClickedAtCacheKey: String)
    fun upsertClick(shortKey: String,totalClicksCacheKey: String, lastClickedAtCacheKey: String, clickSummary: ClickSummary)
    fun findClickSummary(totalClicksCacheKey: String, lastClickedAtCacheKey: String): ClickSummary?
}