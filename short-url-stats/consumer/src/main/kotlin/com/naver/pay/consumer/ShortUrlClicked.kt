package com.naver.pay.consumer

import com.naver.pay.payload.ShortUrlClickedPayload
import com.naver.pay.shorturl.stats.DailyTopStatsService
import com.naver.pay.shorturl.stats.TotalStatsService
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.stereotype.Component
import java.util.function.Consumer
import org.springframework.messaging.Message
import java.time.ZoneId

@Component
class ShortUrlClicked (
    private val totalStatsService: TotalStatsService,
    private val dailyTopStatsService: DailyTopStatsService,
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
        val krDate = payload.clickedAt
            .atZone(ZoneId.of("Asia/Seoul"))
            .toLocalDate()
        totalStatsService.recordClickAtomically(
            shortKey = payload.shortKey,
            referrer = referrer,
            device = device,
            date = krDate,
            clickedAt = payload.clickedAt
        )
        dailyTopStatsService.captureClick(
            date = krDate,
            shortKey = payload.shortKey,
            referrer = referrer,
            device = device
        )
    }

    private fun parseDeviceType(userAgent: String?): String {
        if (userAgent.isNullOrBlank()) return "Unknown"

        val agent = userAgentAnalyzer.parse(userAgent)
        val deviceClass = agent.getValue("DeviceClass")
        return deviceClass
    }
}