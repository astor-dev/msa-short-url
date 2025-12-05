package com.naver.pay.shorturl.stats.mongodb

import com.naver.pay.shorturl.stats.ShortUrlMetadata
import com.naver.pay.shorturl.stats.ShortUrlStatsByDate
import com.naver.pay.shorturl.stats.ShortUrlStatsByDevice
import com.naver.pay.shorturl.stats.ShortUrlStatsByReferrer
import com.naver.pay.shorturl.stats.TotalStats
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "short_url_total_stats")
class ShortUrlTotalStatsDocument (
    @Id
    val shortKey: String,
    val totalClicks: Long = 0,
    // NOTE: yyyy-MM-dd
    val byDate: MutableMap<String, Long> = mutableMapOf(),
    val byDevice: MutableMap<String, Long> = mutableMapOf(),
    val byReferrer: MutableMap<String, Long> = mutableMapOf(),
    val lastClickedAt: Instant? = null,
    val metadata: ShortUrlMetadata,
) {
    fun toDomain(): TotalStats {
        val byDateDomain = this.byDate.map { (date, clicks) ->
            ShortUrlStatsByDate(
                date = date,
                clicks = clicks
            )
        }

        val byDeviceDomain = this.byDevice.map { (deviceType, clicks) ->
            ShortUrlStatsByDevice(
                deviceType = deviceType,
                clicks = clicks
            )
        }

        val byReferrerDomain = this.byReferrer.map { (referrer, clicks) ->
            ShortUrlStatsByReferrer(
                referrer = referrer,
                clicks = clicks
            )
        }

        return TotalStats(
            shortKey = this.shortKey,
            totalClicks = this.totalClicks,
            byDate = byDateDomain,
            byDevice = byDeviceDomain,
            byReferrer = byReferrerDomain,
            lastClickedAt = this.lastClickedAt,
            metadata = this.metadata
        )
    }
}