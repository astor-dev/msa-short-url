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
    private val totalStatsRepository: TotalStatsRepository,
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
     * 
     * 캐시에 기록하며, 배치 작업을 통해 MongoDB에 동기화됩니다.
     *
     * @param shortKey 방문한 url의 short Key
     * @param referrer 헤더로 부터 추출된 referrer
     * @param date LocalDate (한국 기준)
     * @param device 헤더로 부터 추출된 device
     * @param clickedAt 클릭 시각
     */
    fun captureClick(shortKey: String, referrer: String, device: String, date: LocalDate, clickedAt: Instant) {
        totalStatsRepository.recordClickToCache(shortKey, date, device, referrer, clickedAt)
    }

    /**
     * TotalStats를 조회합니다.
     *
     * 캐시를 우선 조회하고, 캐시 미스 시 MongoDB에서 조회 후 캐시에 초기값을 설정합니다.
     *
     * @param shortKey 조회하려는 short Key
     * @return TotalStats, 조회된 데이터가 없으면 null
     */
    fun findOne(shortKey: String): TotalStats? {
        val totalStatsVo = totalStatsRepository.findTotalStatsInCache(shortKey)
        if (totalStatsVo != null) {
            return resolveTotalStats(totalStatsVo)
        }
        return totalStatsRepository.findOneFromDbAndInitializeCache(shortKey)
    }

    /**
     * TotalStatsVo를 도메인 객체로 resolve합니다.
     *
     * metadata는 MongoDB에서 조회합니다.
     *
     * @param totalStatsVo 캐시로부터 조회된 TotalStatsVo
     * @return TotalStats, 조회된 데이터가 없으면 null
     */
    fun resolveTotalStats(totalStatsVo: TotalStatsVo): TotalStats? {
        val document = shortUrlTotalStatsRepository.findByIdOrNull(totalStatsVo.shortKey) ?: return null

        val byDate = totalStatsVo.byDate.map {
            ShortUrlStatsByDate(
                date = it.date,
                clicks = it.clicks
            )
        }

        val byDevice = totalStatsVo.byDevice.map {
            ShortUrlStatsByDevice(
                deviceType = it.deviceType,
                clicks = it.clicks
            )
        }

        val byReferrer = totalStatsVo.byReferrer.map {
            ShortUrlStatsByReferrer(
                referrer = it.referrer,
                clicks = it.clicks
            )
        }

        return TotalStats(
            shortKey = totalStatsVo.shortKey,
            totalClicks = totalStatsVo.totalClicks,
            byDate = byDate,
            byDevice = byDevice,
            byReferrer = byReferrer,
            lastClickedAt = totalStatsVo.lastClickedAt,
            metadata = document.metadata
        )
    }
}