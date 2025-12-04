package com.naver.pay.shorturl.stats.infrastructure.mongodb

import com.naver.pay.shorturl.stats.ShortUrlMetadata
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "short_url_total_stats")
class ShortUrlTotalStatsDocument (
    @Id
    val shortKey: String,
    val totalClicks: Long,
    // NOTE: yyyy-MM-dd
    val byDate: MutableMap<String, Long> = mutableMapOf(),
    val byDevice: MutableMap<String, Long> = mutableMapOf(),
    val byReferrer: MutableMap<String, Long> = mutableMapOf(),
    val lastClickedAt: Instant?,
    val metadata: ShortUrlMetadata,
    val updatedAt: Instant = Instant.now()
)