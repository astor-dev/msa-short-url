package com.naver.pay.shorturl.stats

object CacheNames {
    private const val PREFIX = "short_url_stats"

    const val DAILY_TOP_URLS = "$PREFIX::daily_top_urls"
    const val DAILY_TOP_REFERRERS = "$PREFIX::daily_top_referrers"
    const val DAILY_TOP_DEVICES = "$PREFIX::daily_top_devices"
    const val INFIX_DAILY_TOP_URLS = "daily_top_urls"
    
    const val TOTAL_STATS_TOTAL_CLICKS = "$PREFIX::total_stats_total_clicks"
    const val TOTAL_STATS_BY_DATE = "$PREFIX::total_stats_by_date"
    const val TOTAL_STATS_BY_DEVICE = "$PREFIX::total_stats_by_device"
    const val TOTAL_STATS_BY_REFERRER = "$PREFIX::total_stats_by_referrer"
    const val TOTAL_STATS_LAST_CLICKED_AT = "$PREFIX::total_stats_last_clicked_at"
    const val TOTAL_STATS_DIRTY_SET = "$PREFIX::total_stats_dirty_set"
}