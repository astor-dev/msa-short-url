package com.naver.pay.exception

class ExpiredLinkException(url: String) : RuntimeException("The link '$url' has expired.")
