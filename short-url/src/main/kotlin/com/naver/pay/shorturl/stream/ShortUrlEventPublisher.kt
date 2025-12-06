package com.naver.pay.shorturl.stream

import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ShortUrlEventProducer(
    private val streamBridge: StreamBridge
) {

    fun publishUrlClicked(shortKey: String, referrer: String, userAgent: String) {
        val message = MessageBuilder
            .withPayload(
                ShortUrlClickedPayload(
                    shortKey = shortKey,
                    referrer = referrer,
                    userAgent = userAgent,
                    clickedAt = Instant.now()
                )
            )
            .setHeader(KafkaHeaders.KEY, shortKey)
            .build()
        streamBridge.send(Bindings.SHORT_URL_CLICKED, message)
    }
}