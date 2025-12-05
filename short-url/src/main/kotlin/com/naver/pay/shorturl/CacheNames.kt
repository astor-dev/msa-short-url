package com.naver.pay.shorturl

object CacheNames {
    private const val PREFIX = "short_url"

    const val SHORT_URL_CREATE_LOCK = "$PREFIX::create_lock"
    const val SHORT_URL_BY_ORIGINAL = "$PREFIX::by_original"
    const val SHORT_URL_BY_SHORT_KEY = "$PREFIX::by_short_key"
}