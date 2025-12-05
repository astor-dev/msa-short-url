package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.infrastructure.mongodb.ShortUrlTotalStatsDocument
import com.naver.pay.shorturl.stats.infrastructure.mongodb.ShortUrlTotalStatsRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ShortUrlTotalStatsService(
    private val shortUrlTotalStatsRepository: ShortUrlTotalStatsRepository,
    private val shortUrlStatsCacheService: ShortUrlStatsCacheService
) {

    fun createTotalStatsIfNotExists(shortKey: String, metadata: ShortUrlMetadata) {
        val exist = shortUrlTotalStatsRepository.existsById(shortKey)
        if(!exist) {
            val document = ShortUrlTotalStatsDocument(
                shortKey = shortKey,
                metadata = metadata
            )
            shortUrlTotalStatsRepository.save(document)
        }
    }

    fun click(shortKey: String, referrer: String, device: String, date: String, clickedAt: Instant) {
        shortUrlTotalStatsRepository.recordClickAtomically(shortKey, referrer, device, date, clickedAt)
        shortUrlStatsCacheService.recordClickAtomically(date, shortKey, referrer, device)
    }

    fun findOne(shortKey: String): ShortUrlTotalStats? {
        return shortUrlTotalStatsRepository.findByIdOrNull(shortKey)?.toDomain()
    }
}