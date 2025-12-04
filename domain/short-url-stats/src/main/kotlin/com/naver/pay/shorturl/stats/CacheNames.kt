package com.naver.pay.shorturl.stats

object CacheNames {
    private const val PREFIX = "short_url_stats"

    const val DAILY_TOP_URLS = "$PREFIX::daily_top_urls"
    const val DAILY_TOP_REFERRERS = "$PREFIX::daily_top_referrers"
    const val DAILY_TOP_DEVICES = "$PREFIX::daily_top_devices"
    const val INFIX_DAILY_TOP_URLS = "daily_top_urls"
}