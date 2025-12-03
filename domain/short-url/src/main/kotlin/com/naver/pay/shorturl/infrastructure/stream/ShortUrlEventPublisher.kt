package com.naver.pay.shorturl.infrastructure.stream

import com.naver.pay.shorturl.ShortUrlClickedPayload
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Component
class ShortUrlEventProducer(
    private val streamBridge: StreamBridge
) {

    fun publishUrlClicked(key: String, payload: ShortUrlClickedPayload) {
        val message = MessageBuilder
            .withPayload(payload)
            .setHeader(KafkaHeaders.KEY, key)
            .build()
        streamBridge.send(Bindings.SHORT_URL_CLICKED_BINDING, message)
    }
}