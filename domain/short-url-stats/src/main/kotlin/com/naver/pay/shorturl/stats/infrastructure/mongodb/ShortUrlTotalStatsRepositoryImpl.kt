package com.naver.pay.shorturl.stats.infrastructure.mongodb

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class ShortUrlTotalStatsRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : ShortUrlTotalStatsCustomRepository {
    override fun recordClickAtomically(shortKey: String, referrer: String, device: String, date: String) {
        val increment: Long = 1
        val query = Query.query(Criteria.where("_id").`is`(shortKey))
        val update = Update()
            .inc("totalClicks", increment)
            .inc("byReferrer.$referrer", increment)
            .inc("byDevice.$device", increment)
            .inc("byDate.$date", increment)
            .set("lastClickedAt", Instant.now())
        mongoTemplate.upsert(
            query,
            update,
            ShortUrlTotalStatsDocument::class.java
        )
    }
}