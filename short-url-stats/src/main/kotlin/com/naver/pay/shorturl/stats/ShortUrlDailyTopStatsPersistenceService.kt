package com.naver.pay.shorturl.stats

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
class ShortUrlDailyTopStatsPersistenceService(
    private val dailyTopUrlsRepository: DailyTopUrlsRepository,
    private val dailyTopReferrersRepository: DailyTopReferrersRepository,
    private val dailyTopDevicesRepository: DailyTopDevicesRepository,
    private val dailyTopByDeviceUrlsRepository: DailyTopByDeviceUrlsRepository
) {
    @Transactional
    fun findOne(date: LocalDate, limit: Long): ShortUrlDailyTopStats? {
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

    /**
     * 일 단위 Top N 통계를 영속화합니다.
     * @param shortUrlDailyTopStats 저장할 일 단위 Top N 통계
     */
    @Transactional
    fun save(shortUrlDailyTopStats: ShortUrlDailyTopStats) {
        val date = shortUrlDailyTopStats.date

        val topUrlsDocuments = shortUrlDailyTopStats.topUrls.map { topUrlInfo ->
            DailyTopUrlsDocument.of(date, topUrlInfo)
        }
        dailyTopUrlsRepository.saveAll(topUrlsDocuments)

        val topReferrersDocuments = shortUrlDailyTopStats.topReferrers.map { topReferrerInfo ->
            DailyTopReferrersDocument.of(date, topReferrerInfo)
        }
        dailyTopReferrersRepository.saveAll(topReferrersDocuments)

        val topDevicesDocuments = shortUrlDailyTopStats.topByDevice.map { topByDeviceInfo ->
            DailyTopDevicesDocument.of(date, topByDeviceInfo)
        }
        dailyTopDevicesRepository.saveAll(topDevicesDocuments)

        val topByDeviceUrlsDocuments = shortUrlDailyTopStats.topByDevice.flatMap { topByDeviceInfo ->
            topByDeviceInfo.topUrls.map { topUrlInfo ->
                DailyTopByDeviceUrlsDocument.of(date, topByDeviceInfo.deviceType, topUrlInfo)
            }
        }
        dailyTopByDeviceUrlsRepository.saveAll(topByDeviceUrlsDocuments)
    }
}