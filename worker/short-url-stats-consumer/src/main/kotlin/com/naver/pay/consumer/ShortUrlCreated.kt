package com.naver.pay.consumer

import com.naver.pay.shorturl.ShortUrlCreatedPayload
import com.naver.pay.shorturl.stats.ShortUrlMetadata
import com.naver.pay.shorturl.stats.ShortUrlTotalStatsService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
class ShortUrlCreated(
    private val shortUrlTotalStatsService: ShortUrlTotalStatsService
): Consumer<Message<ShortUrlCreatedPayload>> {
    private val logger = KotlinLogging.logger(ShortUrlCreated::class.java.name)

    init {
        logger.info { "consumer initialized: ${this::class.java.name}" }
    }

    override fun accept(message: Message<ShortUrlCreatedPayload>) {
        val payload = message.payload
        val metadata = ShortUrlMetadata(
            shortUrl = payload.shortUrl,
            originalUrl = payload.originalUrl,
            shortUrlCreatedAt = payload.shortUrlCreatedAt,
            shortUrlExpiredAt = payload.shortUrlExpiredAt
        )

        // NOTE: 재처리 Safe하게 처리
        shortUrlTotalStatsService.createTotalStatsIfNotExists(
            shortKey = payload.shortKey,
            metadata = metadata,
        )
    }

}