package com.naver.pay.shorturl.exception

class ExpiredLinkException(url: String) : RuntimeException("The link '$url' has expired.")