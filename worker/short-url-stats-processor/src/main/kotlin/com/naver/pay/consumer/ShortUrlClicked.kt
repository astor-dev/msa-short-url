package com.naver.pay.consumer

import com.naver.pay.shorturl.ShortUrlClickedPayload
import com.naver.pay.shorturl.resolved.ShortUrlSummaryService
import com.naver.pay.shorturl.stats.ShortUrlTotalStatsService
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.stereotype.Component
import java.util.function.Consumer
import org.springframework.messaging.Message
import java.time.ZoneId

@Component
class ShortUrlClicked (
    private val shortUrlTotalStatsService: ShortUrlTotalStatsService,
    private val shortUrlSummaryService: ShortUrlSummaryService,
    private val userAgentAnalyzer: UserAgentAnalyzer
): Consumer<Message<ShortUrlClickedPayload>> {
    private val logger = KotlinLogging.logger(ShortUrlCreated::class.java.name)

    init {
        logger.info { "consumer initialized: ${this::class.java.name}" }
    }

    override fun accept(message: Message<ShortUrlClickedPayload>) {
        val payload = message.payload
        val referrer = payload.referrer
        val device = parseDeviceType(payload.userAgent)
        val dateString = payload.clickedAt
            .atZone(ZoneId.of("Asia/Seoul"))
            .toLocalDate()
            .toString()
        shortUrlSummaryService.incrementClickCount(payload.shortKey)
        shortUrlTotalStatsService.click(
            shortKey = payload.shortKey,
            referrer = referrer,
            device = device,
            date = dateString,
            clickedAt = payload.clickedAt
        )
    }

    private fun parseDeviceType(userAgent: String?): String {
        if (userAgent.isNullOrBlank()) return "Unknown"

        val agent = userAgentAnalyzer.parse(userAgent)
        val deviceClass = agent.getValue("DeviceClass")
        return deviceClass
    }
}