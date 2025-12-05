package com.naver.pay.shorturl.stats.mongodb

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class ShortUrlTotalStatsRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : ShortUrlTotalStatsCustomRepository {
    override fun recordClickAtomically(shortKey: String, referrer: String, device: String, date: LocalDate, clickedAt: Instant) {
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString()
        val increment: Long = 1
        val query = query(Criteria.where("_id").`is`(shortKey))
        val update = Update()
            .inc("totalClicks", increment)
            .inc("byReferrer.$referrer", increment)
            .inc("byDevice.$device", increment)
            .inc("byDate.$dateString", increment)
            .set("lastClickedAt", clickedAt)
        mongoTemplate.upsert(
            query,
            update,
            ShortUrlTotalStatsDocument::class.java
        )
    }
}