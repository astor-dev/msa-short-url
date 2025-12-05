package com.naver.pay.shorturl.stats.mongodb

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

        fun of(date: String, deviceType: String, topUrlInfo: TopUrlInfo): DailyTopByDeviceUrlsDocument {
            return DailyTopByDeviceUrlsDocument(
                id = generateDocumentId(date, deviceType, topUrlInfo.shortKey),
                date = date,
                deviceType = deviceType,
                rank = topUrlInfo.rank,
                shortKey = topUrlInfo.shortKey,
                totalClicks = 0L, // 전체 클릭 수는 별도로 관리되므로 0으로 설정
                shortUrl = topUrlInfo.shortUrl,
                originalUrl = topUrlInfo.originalUrl,
                clicksFromThisDevice = topUrlInfo.totalClicks,
                lastUpdatedAt = Instant.now()
            )
        }
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