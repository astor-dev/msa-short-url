package com.naver.pay.shorturl.stats

import com.mongodb.DuplicateKeyException
import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsDocument
import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

@Service
class TotalStatsService(
    private val shortUrlTotalStatsRepository: ShortUrlTotalStatsRepository,
) {

    private val logger = KotlinLogging.logger(TotalStatsService::class.java.name)

    /**
     * ShortUrlTotalStatsDocument가 존재하지 않는 경우 생성합니다.
     * 
     * @param shortKey 방문한 url의 short Key
     * @param metadata 방문한 url의 metadata
     */
    fun createTotalStatsIfNotExists(shortKey: String, metadata: ShortUrlMetadata) {
        try {
            val document = ShortUrlTotalStatsDocument(
                shortKey = shortKey,
                metadata = metadata
            )
            shortUrlTotalStatsRepository.insert(document)
        } catch (e: DuplicateKeyException) {
            logger.info(e) { "failed to insert: Duplicated key $shortKey" }
        }
    }

    /**
     * 클릭 수를 totalStats에 원자적으로 기록합니다.
     * TODO: 캐시에 기록 및 동기화하도록 변경
     *
     * @param shortKey 방문한 url의 short Key
     * @param referrer 헤더로 부터 추출된 referrer
     * @param date LocalDate (한국 기준)*
     * @param device 헤더로 부터 추출된 device
     */
    fun captureClick(shortKey: String, referrer: String, device: String, date: LocalDate, clickedAt: Instant) {
        shortUrlTotalStatsRepository.recordClickAtomically(shortKey, referrer, device, date, clickedAt)
    }

    /**
     * ShortUrlTotalStatsDocument를 조회합니다.
     *
     * @param shortKey 조회하려는 short Key
     * @return TotalStats, 조회된 데이터가 없으면 null
     */
    fun findOne(shortKey: String): TotalStats? {
        return shortUrlTotalStatsRepository.findByIdOrNull(shortKey)?.toDomain()
    }
}