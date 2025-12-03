package com.naver.pay.shorturl.resolved

import java.time.Instant

data class ClickSummary (
    val totalClicks: Long = 0,
    val lastClickedAt: Instant? = null
)