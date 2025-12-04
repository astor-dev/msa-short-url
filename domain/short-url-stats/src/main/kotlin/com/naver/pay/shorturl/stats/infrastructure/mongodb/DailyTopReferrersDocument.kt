package com.naver.pay.shorturl.stats.infrastructure.mongodb

import com.naver.pay.shorturl.stats.TopReferrerInfo
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "daily_top_referrers")
@CompoundIndex(name = "date_rank_idx", def = "{'date': 1, 'rank': 1}", background = true)
data class DailyTopReferrersDocument(
    @Id
    val id: String,
    val date: String,
    val rank: Int,
    val referrer: String,
    val totalClicks: Long,
    val lastUpdatedAt: Instant = Instant.now()
) {
    companion object {
        // yyyy-MM-dd
        private fun generateDocumentId(dateString: String, referrer: String) =
            "${dateString}_$referrer"
    }

    fun toDomain(): TopReferrerInfo {
        return TopReferrerInfo(
            rank = this.rank,
            referrer = this.referrer,
            totalClicks = this.totalClicks
        )
    }
}