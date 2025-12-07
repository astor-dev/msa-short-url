package com.naver.pay.filter

object GatewayFilterOrder {
    const val CLIENT_ID = -300
    const val RATE_LIMITING = -200
    const val AUTHENTICATION = -100
    const val AUTHORIZATION = -90
    const val LOGGING = 1000
}
