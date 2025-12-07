package com.naver.pay.shorturl.stats.mongodb

import com.naver.pay.shorturl.stats.TotalStats
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
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

    override fun saveAll(totalStatsList: List<TotalStats>) {
        if (totalStatsList.isEmpty()) {
            return
        }

        val bulkOps = mongoTemplate.bulkOps(
            BulkOperations.BulkMode.ORDERED,
            ShortUrlTotalStatsDocument::class.java
        )

        totalStatsList.forEach { totalStats ->
            val query = Query.query(Criteria.where("_id").`is`(totalStats.shortKey))
            val update = Update()
                .set("totalClicks", totalStats.totalClicks)
                .set("byDate", totalStats.byDate.associate { it.date to it.clicks }.toMutableMap())
                .set("byDevice", totalStats.byDevice.associate { it.deviceType to it.clicks }.toMutableMap())
                .set("byReferrer", totalStats.byReferrer.associate { it.referrer to it.clicks }.toMutableMap())
                .set("lastClickedAt", totalStats.lastClickedAt)
                .set("metadata", totalStats.metadata)

            bulkOps.upsert(query, update)
        }

        bulkOps.execute()
    }
}