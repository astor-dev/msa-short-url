package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.ShortUrlCachableService
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopByDeviceUrlsRepository
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopDevicesRepository
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopReferrersRepository
import com.naver.pay.shorturl.stats.infrastructure.mongodb.DailyTopUrlsRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ShortUrlDailyTopStatsService(
    private val shortUrlStatsCacheService: ShortUrlStatsCacheService,
    private val shortUrlCachableService: ShortUrlCachableService,
    private val dailyTopUrlsRepository: DailyTopUrlsRepository,
    private val dailyTopReferrersRepository: DailyTopReferrersRepository,
    private val dailyTopDevicesRepository: DailyTopDevicesRepository,
    private val dailyTopByDeviceUrlsRepository: DailyTopByDeviceUrlsRepository
) {
    /**
     * 단축 Url에 대한 일 단위 Top N 통계를 조회합니다.
     * 영속화된 통계가 없는 경우 캐시로부터 조회합니다.
     * @param date 조회하려는 일자
     * @param limit top N에서 N의 상한
     * @return ShortUrlDailyTopStats 일 단위 Top N 통계
     */
    fun findOne(date: LocalDate, limit: Long): ShortUrlDailyTopStats {
        val statsFromPersistence =  findOneFromPersistence(date, limit)
        if(statsFromPersistence != null) return statsFromPersistence
        val dailyStatsVo = shortUrlStatsCacheService.getDailyStatistics(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), limit)
        return resolveTotalStats(dailyStatsVo)
    }

    fun findOneFromPersistence(date: LocalDate, limit: Long): ShortUrlDailyTopStats? {
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val pageable = PageRequest.of(0, limit.toInt(), Sort.by(Sort.Direction.ASC, "rank"))

        val topUrlsDocuments = dailyTopUrlsRepository.findByDateOrderByRankAsc(dateString, pageable)
        if (topUrlsDocuments.isEmpty()) {
            return null
        }

        val topReferrersDocuments = dailyTopReferrersRepository.findByDateOrderByRankAsc(dateString, pageable)
        val topDevicesDocuments = dailyTopDevicesRepository.findByDateOrderByTotalClicksDesc(dateString)

        val topUrls = topUrlsDocuments.map { it.toDomain() }
        val topReferrers = topReferrersDocuments.map { it.toDomain() }

        val topByDevice = topDevicesDocuments.map { deviceDocument ->
            val deviceTopUrlsDocuments = dailyTopByDeviceUrlsRepository.findByDateAndDeviceTypeOrderByRankAsc(
                dateString,
                deviceDocument.deviceType,
                pageable
            )
            deviceDocument.toDomain(deviceTopUrlsDocuments.map { it.toDomain() })
        }

        return ShortUrlDailyTopStats(
            date = dateString,
            topUrls = topUrls,
            topReferrers = topReferrers,
            topByDevice = topByDevice
        )
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