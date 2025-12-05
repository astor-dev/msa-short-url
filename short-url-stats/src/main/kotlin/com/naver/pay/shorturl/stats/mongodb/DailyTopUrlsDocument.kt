package com.naver.pay.shorturl.stats.mongodb

import com.naver.pay.shorturl.stats.TopUrlInfo
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

        fun of(date: String, topUrlInfo: TopUrlInfo): DailyTopUrlsDocument {
            return DailyTopUrlsDocument(
                id = generateDocumentId(date, topUrlInfo.shortKey),
                date = date,
                rank = topUrlInfo.rank,
                shortKey = topUrlInfo.shortKey,
                totalClicks = topUrlInfo.totalClicks,
                shortUrl = topUrlInfo.shortUrl,
                originalUrl = topUrlInfo.originalUrl,
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
            totalClicks = this.totalClicks
        )
    }
}
