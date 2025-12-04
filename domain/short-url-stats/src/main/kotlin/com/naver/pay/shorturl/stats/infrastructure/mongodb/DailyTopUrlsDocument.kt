package com.naver.pay.shorturl.stats.infrastructure.mongodb

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "daily_top_urls")
@CompoundIndex(name = "date_rank_idx",   def = "{'date': 1, 'rank': 1}", background = true)
data class DailyTopUrlsDocument(
    @Id
    val id: String,
    val date: String,
    val rank: Int,
    val shortKey: String,
    val totalClicks: Long,
    val shortUrl: String,
    val originalUrl: String,
    val lastUpdatedAt: Instant = Instant.now()
) {
    companion object {
        // yyyy-MM-dd
        private fun generateDocumentId(dateString: String, shortKey: String) =
            "${dateString}_$shortKey"
    }
}
