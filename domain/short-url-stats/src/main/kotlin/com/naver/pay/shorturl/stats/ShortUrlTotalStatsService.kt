package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.infrastructure.mongodb.ShortUrlTotalStatsDocument
import com.naver.pay.shorturl.stats.infrastructure.mongodb.ShortUrlTotalStatsRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ShortUrlTotalStatsService(
    private val shortUrlTotalStatsRepository: ShortUrlTotalStatsRepository
) {

    fun createTotalStats(shortKey: String, metadata: ShortUrlMetadata) {
        val document = ShortUrlTotalStatsDocument(
            shortKey = shortKey,
            metadata = metadata
        )
        shortUrlTotalStatsRepository.save(document)
    }

    fun click(shortKey: String, referrer: String, device: String, date: String, clickedAt: Instant) {
        shortUrlTotalStatsRepository.recordClickAtomically(shortKey, referrer, device, date, clickedAt)
    }

    fun findOne(shortKey: String): ShortUrlTotalStats? {
        return shortUrlTotalStatsRepository.findByIdOrNull(shortKey)?.toDomain()
    }
}