package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.mongodb.DailyTopByDeviceUrlsDocument
import com.naver.pay.shorturl.stats.mongodb.DailyTopByDeviceUrlsRepository
import com.naver.pay.shorturl.stats.mongodb.DailyTopDevicesDocument
import com.naver.pay.shorturl.stats.mongodb.DailyTopDevicesRepository
import com.naver.pay.shorturl.stats.mongodb.DailyTopReferrersDocument
import com.naver.pay.shorturl.stats.mongodb.DailyTopReferrersRepository
import com.naver.pay.shorturl.stats.mongodb.DailyTopUrlsDocument
import com.naver.pay.shorturl.stats.mongodb.DailyTopUrlsRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Collections

@Service
class DailyTopStatsRepository(
    private val dailyTopUrlsRepository: DailyTopUrlsRepository,
    private val dailyTopReferrersRepository: DailyTopReferrersRepository,
    private val dailyTopDevicesRepository: DailyTopDevicesRepository,
    private val dailyTopByDeviceUrlsRepository: DailyTopByDeviceUrlsRepository,
    private val redisTemplate: RedisTemplate<String, String>
) {
    @Transactional
    fun findOne(date: LocalDate, limit: Long): DailyTopStats? {
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

        return DailyTopStats(
            date = dateString,
            topUrls = topUrls,
            topReferrers = topReferrers,
            topByDevice = topByDevice
        )
    }

    /**
     * 일 단위 Top N 통계를 영속화합니다.
     * @param dailyTopStats 저장할 일 단위 Top N 통계
     */
    @Transactional
    fun save(dailyTopStats: DailyTopStats) {
        val date = dailyTopStats.date

        val topUrlsDocuments = dailyTopStats.topUrls.map { topUrlInfo ->
            DailyTopUrlsDocument.of(date, topUrlInfo)
        }
        dailyTopUrlsRepository.saveAll(topUrlsDocuments)

        val topReferrersDocuments = dailyTopStats.topReferrers.map { topReferrerInfo ->
            DailyTopReferrersDocument.of(date, topReferrerInfo)
        }
        dailyTopReferrersRepository.saveAll(topReferrersDocuments)

        val topDevicesDocuments = dailyTopStats.topByDevice.map { topByDeviceInfo ->
            DailyTopDevicesDocument.of(date, topByDeviceInfo)
        }
        dailyTopDevicesRepository.saveAll(topDevicesDocuments)

        val topByDeviceUrlsDocuments = dailyTopStats.topByDevice.flatMap { topByDeviceInfo ->
            topByDeviceInfo.topUrls.map { topUrlInfo ->
                DailyTopByDeviceUrlsDocument.of(date, topByDeviceInfo.deviceType, topUrlInfo)
            }
        }
        dailyTopByDeviceUrlsRepository.saveAll(topByDeviceUrlsDocuments)
    }

    private final val ttlSeconds: Long = Duration.ofHours(48).toSeconds()

    /**
     * 클릭 통계 기록 (ZINCRBY) 및 만료 시간 (EXPIRE) 설정을 위한 Redis Lua 스크립트
     * KEYS[1]: globalUrlKey
     * KEYS[2]: referrerKey
     * KEYS[3]: deviceParentKey
     * KEYS[4]: deviceChildKey
     * ARGV[1]: shortKey
     * ARGV[2]: referrer
     * ARGV[3]: device
     * ARGV[4]: 만료 시간(초)
     */
    private final val recordStatsScript: RedisScript<Void> = DefaultRedisScript(
        """
        redis.call('ZINCRBY', KEYS[1], 1, ARGV[1])
        redis.call('EXPIRE', KEYS[1], ARGV[4])
        redis.call('ZINCRBY', KEYS[2], 1, ARGV[2])
        redis.call('EXPIRE', KEYS[2], ARGV[4])
        redis.call('ZINCRBY', KEYS[3], 1, ARGV[3])
        redis.call('EXPIRE', KEYS[3], ARGV[4])
        redis.call('ZINCRBY', KEYS[4], 1, ARGV[1])
        redis.call('EXPIRE', KEYS[4], ARGV[4])
        """,
        Void::class.java
    )

    fun recordClickAtomically(
        date: LocalDate,
        shortKey: String,
        referrer: String,
        device: String
    ) {
        val dateKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString()
        val urlKey = "${CacheNames.DAILY_TOP_URLS}::$dateKey"
        val referrerKey = "${CacheNames.DAILY_TOP_REFERRERS}::$dateKey"
        val deviceParentKey = "${CacheNames.DAILY_TOP_DEVICES}::$dateKey"
        val deviceChildKey = "${CacheNames.DAILY_TOP_DEVICES}::$dateKey::${CacheNames.INFIX_DAILY_TOP_URLS}::$device"

        val keys = listOf(urlKey, referrerKey, deviceParentKey, deviceChildKey)
        val args = arrayOf(shortKey, referrer, device, ttlSeconds.toString())

        redisTemplate.execute(
            recordStatsScript,
            keys,
            *args,
        )
    }

    fun findDailyStatsInCache(dateKey: String, limit: Long): DailyStatsVo? {
        val urlKey = "${CacheNames.DAILY_TOP_URLS}::$dateKey"
        if(!redisTemplate.hasKey(urlKey)) return null
        val topUrls = getTopRank(urlKey, limit).mapNotNull { toKeyCount(it) }
        val referrerKey = "${CacheNames.DAILY_TOP_REFERRERS}::$dateKey"
        val topReferrers = getTopRank(referrerKey, limit).mapNotNull { toKeyCount(it) }
        val deviceParentKey = "${CacheNames.DAILY_TOP_DEVICES}::$dateKey"
        val deviceParentTuples = getTopRank(deviceParentKey, -1)

        // Parent에서 얻은 device 목록을 순회하며 Child Key 조회
        val topByDevice = deviceParentTuples.map { parentTuple ->
            val deviceType = parentTuple.value ?: "Unknown"
            val childKey = "${CacheNames.DAILY_TOP_DEVICES}::$dateKey::${CacheNames.INFIX_DAILY_TOP_URLS}::$deviceType"
            val childUrls = getTopRank(childKey, limit).mapNotNull { toKeyCount(it) }
            DeviceStatsVo(
                deviceType = deviceType,
                totalCount = parentTuple.score?.toLong() ?: 0L,
                topUrls = childUrls
            )
        }

        return DailyStatsVo(
            dateKey = dateKey,
            topUrls = topUrls,
            topReferrers = topReferrers,
            topByDevice = topByDevice
        )
    }

    private fun getTopRank(key: String, limit: Long): Set<ZSetOperations.TypedTuple<String>> {
        val end = if (limit < 0) -1 else limit - 1
        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, 0, end) ?: Collections.emptySet()
    }

    private fun toKeyCount(tuple: ZSetOperations.TypedTuple<String>): KeyCountVo? {
        return tuple.value?.let {
            KeyCountVo(
                key = it,
                count = tuple.score?.toLong() ?: 0
            )
        }
    }
}