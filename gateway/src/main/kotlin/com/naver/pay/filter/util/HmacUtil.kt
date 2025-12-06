package com.naver.pay.filter.util

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@ConfigurationProperties(prefix = "security.hmac")
data class HmacProperties(
    val secret: String
)

fun hmacSha256(data: String, hmacProperties: HmacProperties): String {
    val mac = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(hmacProperties.secret.toByteArray(), "HmacSHA256")
    mac.init(secretKey)
    val hash = mac.doFinal(data.toByteArray())
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
}