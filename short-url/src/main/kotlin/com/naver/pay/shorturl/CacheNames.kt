package com.naver.pay.shorturl

object CacheNames {
    private const val PREFIX = "short_url"

    const val SHORT_URL_CREATE_LOCK = "$PREFIX::create_lock"
    const val SHORT_URL_GET_LOCK = "$PREFIX::get_lock"
    const val SHORT_URL_BY_ORIGINAL = "$PREFIX::by_original"
    const val REDIRECT_URL_BY_SHORT_KEY = "$PREFIX::redirect_url_by_short_key"
    const val EXPIRES_AT_BY_SHORT_KEY = "$PREFIX::expires_at_by_short_key"
}