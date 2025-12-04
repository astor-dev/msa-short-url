package com.naver.pay.shorturl.stats.infrastructure.mongodb

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
    }
}