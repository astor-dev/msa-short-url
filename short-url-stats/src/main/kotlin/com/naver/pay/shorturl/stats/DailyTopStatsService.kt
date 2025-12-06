package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DailyTopStatsService(
    private val dailyTopStatsRepository: DailyTopStatsRepository,
    private val shortUrlTotalStatsRepository: ShortUrlTotalStatsRepository
) {
    /**
     * 단축 Url에 대한 일 단위 Top N 통계를 조회합니다.
     *
     * 캐시를 우선 조회하고, 영속화된 데이터를 찾습니다.
     * 해당 일자에 저장된 통계가 없는 경우, 빈 리스트를 지닌 응답 객체를 반환합니다.
     *
     * @param date 조회하려는 일자
     * @param limit top N에서 N의 상한
     * @return 일 단위 Top N 통계
     */
    fun getOne(date: LocalDate, limit: Long): DailyTopStats {
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val dailyStatsVo = dailyTopStatsRepository.findDailyStatsInCache(dateString, limit)
        if (dailyStatsVo != null) {
            return resolveTotalStats(dailyStatsVo)
        }
        val statsFromPersistence = dailyTopStatsRepository.findOne(date, limit)
        return statsFromPersistence ?: DailyTopStats(date = dateString)
    }

    /**
     * 클릭 수를 캐시에 원자적으로 캡쳐합니다.
     * 2일 이후 캐시가 만료됩니다.
     *
     * @param date LocalDate (한국 기준)
     * @param shortKey 방문한 url의 short Key
     * @param referrer 헤더로 부터 추출된 referrer
     * @param device 헤더로 부터 추출된 device
     */
    fun captureClick(
        date: LocalDate,
        shortKey: String,
        referrer: String,
        device: String
    ) {
        return this.dailyTopStatsRepository.recordClickToCache(date, shortKey, referrer, device)
    }

    /**
     * dailyStatsVo를 도메인 클래스로 resolve 합니다.
     * totalStatsDocument의 메타데이터를 활용합니다.
     *
     * @throws NoSuchElementException shortKey에 해당하는 document가 없는 경우
     * @param dailyStatsVo 캐시로부터 조회된 dailyStats
     */
    fun resolveTotalStats(dailyStatsVo: DailyStatsVo): DailyTopStats {
        val allUniqueKeys = (
                dailyStatsVo.topUrls.map { it.key } +
                        dailyStatsVo.topByDevice.flatMap { it.topUrls.map { url -> url.key } }
                ).toSet()
        val documents = shortUrlTotalStatsRepository.findAllById(allUniqueKeys)
        val shortUrlMap = documents.associateBy { it.shortKey }

        fun mapToTopUrlInfo(rank: Int, keyCount: KeyCountVo): TopUrlInfo {
            val shortUrl = shortUrlMap[keyCount.key]
                ?: throw NoSuchElementException("$keyCount key does not exist")
            return TopUrlInfo(
                rank = rank,
                shortKey = keyCount.key,
                shortUrl = shortUrl.metadata.shortUrl,
                originalUrl = shortUrl.metadata.originalUrl,
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