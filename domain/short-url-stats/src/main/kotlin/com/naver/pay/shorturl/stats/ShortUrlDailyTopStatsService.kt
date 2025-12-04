package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.ShortUrlCachableService
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopByDeviceUrlsDocument
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopByDeviceUrlsRepository
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopDevicesDocument
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopDevicesRepository
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopReferrersDocument
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopReferrersRepository
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopUrlsDocument
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopUrlsRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ShortUrlDailyTopStatsService(
    private val shortUrlStatsCacheService: ShortUrlStatsCacheService,
    private val shortUrlCachableService: ShortUrlCachableService,
    private val shortUrlDailyTopStatsPersistenceService: ShortUrlDailyTopStatsPersistenceService
) {
    /**
     * 단축 Url에 대한 일 단위 Top N 통계를 조회합니다.
     * 영속화된 통계가 없는 경우 캐시로부터 조회합니다.
     * @param date 조회하려는 일자
     * @param limit top N에서 N의 상한
     * @return ShortUrlDailyTopStats 일 단위 Top N 통계
     */
    fun findOne(date: LocalDate, limit: Long): ShortUrlDailyTopStats {
        val statsFromPersistence =  shortUrlDailyTopStatsPersistenceService.findOne(date, limit)
        if(statsFromPersistence != null) return statsFromPersistence
        val dailyStatsVo = shortUrlStatsCacheService.getDailyStatistics(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), limit)
        return resolveTotalStats(dailyStatsVo)
    }



    fun resolveTotalStats(dailyStatsVo: DailyStatsVo): ShortUrlDailyTopStats {
        val allUniqueKeys = (
                dailyStatsVo.topUrls.map { it.key } +
                        dailyStatsVo.topByDevice.flatMap { it.topUrls.map { url -> url.key } }
                ).toSet()
        // TODO: In 활용 조회로 최적화
        val shortUrlMap = allUniqueKeys.associateWith { key ->
            shortUrlCachableService.findShortUrlByShortKeyOrThrow(key)
        }

        fun mapToTopUrlInfo(rank: Int, keyCount: KeyCountVo): TopUrlInfo {
            val shortUrl = shortUrlMap[keyCount.key]
                ?: throw IllegalStateException("ShortUrl info not found for key: ${keyCount.key}")
            val fullShortUrl = shortUrl.getShortUrl()
            return TopUrlInfo(
                rank = rank,
                shortKey = keyCount.key,
                shortUrl = fullShortUrl,
                originalUrl = shortUrl.originalUrl,
                totalClicks = keyCount.count
            )
        }

        val topUrls = dailyStatsVo.topUrls.mapIndexed { index, vo ->
            mapToTopUrlInfo(index + 1, vo)
        }
        val topReferrers = dailyStatsVo.topReferrers.mapIndexed { index, vo ->
            TopReferrerInfo(
                rank = index + 1,
                referrer = vo.key,
                totalClicks = vo.count
            )
        }
        val topByDevice = dailyStatsVo.topByDevice.map { deviceVo ->
            TopByDeviceInfo(
                deviceType = deviceVo.deviceType,
                totalClicks = deviceVo.totalCount,
                topUrls = deviceVo.topUrls.mapIndexed { index, vo ->
                    mapToTopUrlInfo(index + 1, vo)
                }
            )
        }
        return ShortUrlDailyTopStats(
            date = dailyStatsVo.dateKey,
            topUrls = topUrls,
            topReferrers = topReferrers,
            topByDevice = topByDevice
        )
    }
}