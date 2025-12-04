package com.naver.pay.shorturl.stats.infrastructure.mongodb

import com.naver.pay.shorturl.stats.TopUrlInfo
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "daily_top_by_device_urls")
@CompoundIndex(name = "date_device_rank_idx", def = "{'date': 1, 'deviceType': 1, 'rank': 1}", background = true)
data class DailyTopByDeviceUrlsDocument(
    @Id
    val id: String,
    val date: String,
    val deviceType: String,
    val rank: Int,
    val shortKey: String,
    val totalClicks: Long,
    val shortUrl: String,
    val originalUrl: String,
    val clicksFromThisDevice: Long,
    val lastUpdatedAt: Instant = Instant.now()
) {
    companion object {
        // yyyy-MM-dd
        private fun generateDocumentId(dateString: String, deviceType: String, shortKey: String) =
            "${dateString}_${deviceType}_$shortKey"
    }

    fun toDomain(): TopUrlInfo {
        return TopUrlInfo(
            rank = this.rank,
            shortKey = this.shortKey,
            shortUrl = this.shortUrl,
            originalUrl = this.originalUrl,
            totalClicks = this.clicksFromThisDevice
        )
    }
}