package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.ShortUrlRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DailyTopStatsService(
    private val shortUrlRepository: ShortUrlRepository,
    private val dailyTopStatsRepository: DailyTopStatsRepository
) {
    /**
     * 단축 Url에 대한 일 단위 Top N 통계를 조회합니다.
     * 영속화된 통계가 없는 경우 캐시로부터 조회합니다.
     * @param date 조회하려는 일자
     * @param limit top N에서 N의 상한
     * @return ShortUrlDailyTopStats 일 단위 Top N 통계
     */
    fun getOne(date: LocalDate, limit: Long): DailyTopStats {
        val statsFromPersistence =  dailyTopStatsRepository.findOne(date, limit)
        if(statsFromPersistence != null) return statsFromPersistence
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val dailyStatsVo = dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
            ?: return DailyTopStats(date = dateString)
        return resolveTotalStats(dailyStatsVo)
    }

    /**
     * 클릭 수를 dailyTopStats에 원자적으로 기록합니다.
     * @param date LocalDate (한국 기준)
     * @param shortKey 방문한 url의 short Key
     * @param referrer 헤더로 부터 추출된 referrer
     * @param device 헤더로 부터 추출된 device
     */
    fun recordClickAtomically(
        date: LocalDate,
        shortKey: String,
        referrer: String,
        device: String
    ) {
        return this.dailyTopStatsRepository.recordClickAtomically(date, shortKey, referrer, device)
    }

    /**
     * dailyStatsVo를 도메인 클래스로 resolve 합니다.
     * shortUrl 모듈 의존성. TODO: 비동기 결합
     */
    fun resolveTotalStats(dailyStatsVo: DailyStatsVo): DailyTopStats {
        val allUniqueKeys = (
                dailyStatsVo.topUrls.map { it.key } +
                        dailyStatsVo.topByDevice.flatMap { it.topUrls.map { url -> url.key } }
                ).toSet()
        // TODO: In 활용 조회로 최적화
        val shortUrlMap = allUniqueKeys.associateWith { key ->
            shortUrlRepository.findShortUrlByShortKey(key)
        }

        fun mapToTopUrlInfo(rank: Int, keyCount: KeyCountVo): TopUrlInfo {
            val shortUrl = shortUrlMap[keyCount.key]
                ?: throw IllegalStateException("ShortUrl info not found for key: ${keyCount.key}")
            val fullShortUrl = shortUrl.generateShortUrlOrThrow()
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
        return DailyTopStats(
            date = dailyStatsVo.dateKey,
            topUrls = topUrls,
            topReferrers = topReferrers,
            topByDevice = topByDevice
        )
    }
}