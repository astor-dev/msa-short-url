package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsDocument
import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

@Service
class TotalStatsService(
    private val shortUrlTotalStatsRepository: ShortUrlTotalStatsRepository,
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

    /**
     * 클릭 수를 totalStats에 원자적으로 기록합니다.
     * @param shortKey 방문한 url의 short Key
     * @param referrer 헤더로 부터 추출된 referrer
     * @param date LocalDate (한국 기준)*
     * @param device 헤더로 부터 추출된 device
     */
    fun recordClickAtomically(shortKey: String, referrer: String, device: String, date: LocalDate, clickedAt: Instant) {
        shortUrlTotalStatsRepository.recordClickAtomically(shortKey, referrer, device, date, clickedAt)
    }

    fun findOne(shortKey: String): TotalStats? {
        return shortUrlTotalStatsRepository.findByIdOrNull(shortKey)?.toDomain()
    }
}