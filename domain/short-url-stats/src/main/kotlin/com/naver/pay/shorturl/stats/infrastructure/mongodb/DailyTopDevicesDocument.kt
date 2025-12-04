package com.naver.pay.shorturl.stats.infrastructure.mongodb

import com.naver.pay.shorturl.stats.TopByDeviceInfo
import com.naver.pay.shorturl.stats.TopUrlInfo
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "daily_top_devices")
@CompoundIndex(name = "date_clicks_idx", def = "{'date': 1, 'totalClicks': -1}", background = true)
data class DailyTopDevicesDocument(
    @Id
    val id: String,
    val date: String,
    val deviceType: String,
    val totalClicks: Long,
    val lastUpdatedAt: Instant = Instant.now()
) {
    companion object {
        // yyyy-MM-dd
        private fun generateDocumentId(dateString: String, deviceType: String) =
            "${dateString}_$deviceType"

        fun of(date: String, topByDeviceInfo: TopByDeviceInfo): DailyTopDevicesDocument {
            return DailyTopDevicesDocument(
                id = generateDocumentId(date, topByDeviceInfo.deviceType),
                date = date,
                deviceType = topByDeviceInfo.deviceType,
                totalClicks = topByDeviceInfo.totalClicks,
                lastUpdatedAt = Instant.now()
            )
        }
    }

    fun toDomain(topUrls: List<TopUrlInfo>): TopByDeviceInfo {
        return TopByDeviceInfo(
            deviceType = this.deviceType,
            totalClicks = this.totalClicks,
            topUrls = topUrls
        )
    }
}