package com.naver.pay.shorturl.stats

import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsDocument
import com.naver.pay.shorturl.stats.mongodb.ShortUrlTotalStatsRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class TotalStatsRepository(
    private val shortUrlTotalStatsRepository: ShortUrlTotalStatsRepository,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private final val ttlSeconds: Long = Duration.ofDays(7).toSeconds()

    /**
     * 클릭 통계 기록 (INCR, HINCRBY) 및 만료 시간 (EXPIRE) 설정을 위한 Redis Lua 스크립트
     * KEYS[1]: totalClicksKey
     * KEYS[2]: byDateKey
     * KEYS[3]: byDeviceKey
     * KEYS[4]: byReferrerKey
     * KEYS[5]: lastClickedAtKey
     * ARGV[1]: date (yyyy-MM-dd)
     * ARGV[2]: device
     * ARGV[3]: referrer
     * ARGV[4]: lastClickedAt (ISO-8601)
     * ARGV[5]: 만료 시간(초)
     */
    private final val recordClickScript: RedisScript<Void> = DefaultRedisScript(
        """
        redis.call('INCR', KEYS[1])
        redis.call('EXPIRE', KEYS[1], ARGV[5])
        redis.call('HINCRBY', KEYS[2], ARGV[1], 1)
        redis.call('EXPIRE', KEYS[2], ARGV[5])
        redis.call('HINCRBY', KEYS[3], ARGV[2], 1)
        redis.call('EXPIRE', KEYS[3], ARGV[5])
        redis.call('HINCRBY', KEYS[4], ARGV[3], 1)
        redis.call('EXPIRE', KEYS[4], ARGV[5])
        redis.call('SET', KEYS[5], ARGV[4])
        redis.call('EXPIRE', KEYS[5], ARGV[5])
        """,
        Void::class.java
    )

    /**
     * click에 대한 통계를 캐시에 원자적으로 기록합니다.
     *
     * @param shortKey 방문한 url의 short Key
     * @param date 클릭 일자
     * @param device 헤더로 부터 추출된 device
     * @param referrer 헤더로 부터 추출된 referrer
     * @param clickedAt 클릭 시각
     */
    fun recordClickToCache(
        shortKey: String,
        date: LocalDate,
        device: String,
        referrer: String,
        clickedAt: Instant
    ) {
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val totalClicksKey = "${CacheNames.TOTAL_STATS_TOTAL_CLICKS}::$shortKey"
        val byDateKey = "${CacheNames.TOTAL_STATS_BY_DATE}::$shortKey"
        val byDeviceKey = "${CacheNames.TOTAL_STATS_BY_DEVICE}::$shortKey"
        val byReferrerKey = "${CacheNames.TOTAL_STATS_BY_REFERRER}::$shortKey"
        val lastClickedAtKey = "${CacheNames.TOTAL_STATS_LAST_CLICKED_AT}::$shortKey"

        val keys = listOf(totalClicksKey, byDateKey, byDeviceKey, byReferrerKey, lastClickedAtKey)
        val args = arrayOf(
            dateString,
            device,
            referrer,
            clickedAt.toString(),
            ttlSeconds.toString()
        )

        redisTemplate.execute(
            recordClickScript,
            keys,
            *args
        )
    }

    /**
     * MongoDB에서 TotalStats를 조회하고 캐시에 초기값을 설정합니다.
     *
     * @param shortKey 조회하려는 short Key
     * @return TotalStats, 조회된 데이터가 없으면 null
     */
    fun findOneFromDbAndInitializeCache(shortKey: String): TotalStats? {
        val document = shortUrlTotalStatsRepository.findByIdOrNull(shortKey) ?: return null
        val totalStats = document.toDomain()

        // 캐시에 초기값 설정
        initializeCache(shortKey, totalStats)

        return totalStats
    }

    /**
     * TotalStats를 캐시에 초기값으로 설정합니다.
     *
     * @param shortKey short Key
     * @param totalStats 초기화할 TotalStats
     */
    private fun initializeCache(shortKey: String, totalStats: TotalStats) {
        val totalClicksKey = "${CacheNames.TOTAL_STATS_TOTAL_CLICKS}::$shortKey"
        val byDateKey = "${CacheNames.TOTAL_STATS_BY_DATE}::$shortKey"
        val byDeviceKey = "${CacheNames.TOTAL_STATS_BY_DEVICE}::$shortKey"
        val byReferrerKey = "${CacheNames.TOTAL_STATS_BY_REFERRER}::$shortKey"
        val lastClickedAtKey = "${CacheNames.TOTAL_STATS_LAST_CLICKED_AT}::$shortKey"

        redisTemplate.opsForValue().set(totalClicksKey, totalStats.totalClicks.toString())
        redisTemplate.expire(totalClicksKey, Duration.ofSeconds(ttlSeconds))

        val byDateMap = totalStats.byDate.associate { it.date to it.clicks.toString() }
        if (byDateMap.isNotEmpty()) {
            redisTemplate.opsForHash<String, String>().putAll(byDateKey, byDateMap)
            redisTemplate.expire(byDateKey, Duration.ofSeconds(ttlSeconds))
        }

        val byDeviceMap = totalStats.byDevice.associate { it.deviceType to it.clicks.toString() }
        if (byDeviceMap.isNotEmpty()) {
            redisTemplate.opsForHash<String, String>().putAll(byDeviceKey, byDeviceMap)
            redisTemplate.expire(byDeviceKey, Duration.ofSeconds(ttlSeconds))
        }

        val byReferrerMap = totalStats.byReferrer.associate { it.referrer to it.clicks.toString() }
        if (byReferrerMap.isNotEmpty()) {
            redisTemplate.opsForHash<String, String>().putAll(byReferrerKey, byReferrerMap)
            redisTemplate.expire(byReferrerKey, Duration.ofSeconds(ttlSeconds))
        }

        totalStats.lastClickedAt?.let {
            redisTemplate.opsForValue().set(lastClickedAtKey, it.toString())
            redisTemplate.expire(lastClickedAtKey, Duration.ofSeconds(ttlSeconds))
        }
    }

    /**
     * 캐시에서 TotalStatsVo를 조회합니다.
     *
     * @param shortKey 조회하려는 short Key
     * @return TotalStatsVo, 조회된 데이터가 없으면 null
     */
    fun findTotalStatsInCache(shortKey: String): TotalStatsVo? {
        val totalClicksKey = "${CacheNames.TOTAL_STATS_TOTAL_CLICKS}::$shortKey"
        if (!redisTemplate.hasKey(totalClicksKey)) {
            return null
        }

        val totalClicks = redisTemplate.opsForValue().get(totalClicksKey)?.toLongOrNull() ?: 0L

        val byDateKey = "${CacheNames.TOTAL_STATS_BY_DATE}::$shortKey"
        val byDateMap = redisTemplate.opsForHash<String, String>().entries(byDateKey)
        val byDate = byDateMap.map { (date, clicks) ->
            DateCountVo(
                date = date,
                clicks = clicks.toLongOrNull() ?: 0L
            )
        }

        val byDeviceKey = "${CacheNames.TOTAL_STATS_BY_DEVICE}::$shortKey"
        val byDeviceMap = redisTemplate.opsForHash<String, String>().entries(byDeviceKey)
        val byDevice = byDeviceMap.map { (deviceType, clicks) ->
            DeviceCountVo(
                deviceType = deviceType,
                clicks = clicks.toLongOrNull() ?: 0L
            )
        }

        val byReferrerKey = "${CacheNames.TOTAL_STATS_BY_REFERRER}::$shortKey"
        val byReferrerMap = redisTemplate.opsForHash<String, String>().entries(byReferrerKey)
        val byReferrer = byReferrerMap.map { (referrer, clicks) ->
            ReferrerCountVo(
                referrer = referrer,
                clicks = clicks.toLongOrNull() ?: 0L
            )
        }

        val lastClickedAtKey = "${CacheNames.TOTAL_STATS_LAST_CLICKED_AT}::$shortKey"
        val lastClickedAtString = redisTemplate.opsForValue().get(lastClickedAtKey)
        val lastClickedAt = lastClickedAtString?.let { Instant.parse(it) }

        return TotalStatsVo(
            shortKey = shortKey,
            totalClicks = totalClicks,
            byDate = byDate,
            byDevice = byDevice,
            byReferrer = byReferrer,
            lastClickedAt = lastClickedAt
        )
    }

    /**
     * TotalStats를 MongoDB에 저장합니다.
     *
     * @param totalStats 저장할 TotalStats
     */
    fun save(totalStats: TotalStats) {
        val document = ShortUrlTotalStatsDocument(
            shortKey = totalStats.shortKey,
            totalClicks = totalStats.totalClicks,
            byDate = totalStats.byDate.associate { it.date to it.clicks }.toMutableMap(),
            byDevice = totalStats.byDevice.associate { it.deviceType to it.clicks }.toMutableMap(),
            byReferrer = totalStats.byReferrer.associate { it.referrer to it.clicks }.toMutableMap(),
            lastClickedAt = totalStats.lastClickedAt,
            metadata = totalStats.metadata
        )

        shortUrlTotalStatsRepository.save(document)
    }
}
