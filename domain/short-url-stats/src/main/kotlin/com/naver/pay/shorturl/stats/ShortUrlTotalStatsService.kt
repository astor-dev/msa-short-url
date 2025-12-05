package com.naver.pay.shorturl.stats

import com.mongodb.DuplicateKeyException
import com.naver.pay.shorturl.stats.infrastructure.mongodb.ShortUrlTotalStatsDocument
import com.naver.pay.shorturl.stats.infrastructure.mongodb.ShortUrlTotalStatsRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ShortUrlTotalStatsService(
    private val shortUrlTotalStatsRepository: ShortUrlTotalStatsRepository,
    private val shortUrlStatsCacheService: ShortUrlStatsCacheService
) {

    @Transactional
    fun createTotalStatsIfNotExists(shortKey: String, metadata: ShortUrlMetadata) {
        val document = ShortUrlTotalStatsDocument(
            shortKey = shortKey,
            metadata = metadata
        )
        try {
            shortUrlTotalStatsRepository.insert(document)
        } catch (e: DuplicateKeyException) {
            return
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