package com.naver.pay.shorturl.resolved

object CacheNames {
    private const val PREFIX = "resolved_short_url"

    const val SHORT_URL_TOTAL_CLICKS = "$PREFIX::total_clicks"
    const val SHORT_URL_LAST_CLICKED_AT = "$PREFIX::last_clicked_at"
    const val RESOLVED_SHORT_URL_BY_SHORT_KEY = "$PREFIX::by_short_key"
}