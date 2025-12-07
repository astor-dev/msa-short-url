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
import java.util.Arrays

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
        val cacheKeys = TotalStatsCacheKeys.from(shortKey)

        val keys = listOf(
            cacheKeys.totalClicksKey,
            cacheKeys.byDateKey,
            cacheKeys.byDeviceKey,
            cacheKeys.byReferrerKey,
            cacheKeys.lastClickedAtKey,
        )
        val args = arrayOf(
            dateString,
            device,
            referrer,
            clickedAt.toString(),
            ttlSeconds.toString(),
        )
        print(keys)
        print(args.contentToString())

        redisTemplate.execute(
            recordClickScript,
            keys,
            *args
        )
        redisTemplate.opsForSet().add(cacheKeys.dirtySetKey, shortKey)
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
        // 1. 저장할 데이터 미리 가공 (Pipeline 내부 로직을 깔끔하게 하기 위해)
        val byDateMap = totalStats.byDate.associate { it.date to it.clicks.toString() }
        val byDeviceMap = totalStats.byDevice.associate { it.deviceType to it.clicks.toString() }
        val byReferrerMap = totalStats.byReferrer.associate { it.referrer to it.clicks.toString() }
        val lastClickedAtStr = totalStats.lastClickedAt?.toString()
        val ttl = Duration.ofSeconds(ttlSeconds)

        // 2. 파이프라인 실행 (반환값 불필요)
        redisTemplate.executePipelined { _ ->
            val cacheKeys = TotalStatsCacheKeys.from(shortKey)

            // Operations
            val valueOps = redisTemplate.opsForValue()
            val hashOps = redisTemplate.opsForHash<String, String>()
            val setOps = redisTemplate.opsForSet()

            // Total Clicks
            valueOps.set(cacheKeys.totalClicksKey, totalStats.totalClicks.toString())
            redisTemplate.expire(cacheKeys.totalClicksKey, ttl)

            // By Date
            if (byDateMap.isNotEmpty()) {
                hashOps.putAll(cacheKeys.byDateKey, byDateMap)
                redisTemplate.expire(cacheKeys.byDateKey, ttl)
            }

            // By Device
            if (byDeviceMap.isNotEmpty()) {
                hashOps.putAll(cacheKeys.byDeviceKey, byDeviceMap)
                redisTemplate.expire(cacheKeys.byDeviceKey, ttl)
            }

            // By Referrer
            if (byReferrerMap.isNotEmpty()) {
                hashOps.putAll(cacheKeys.byReferrerKey, byReferrerMap)
                redisTemplate.expire(cacheKeys.byReferrerKey, ttl)
            }

            // Last Clicked At
            if (lastClickedAtStr != null) {
                valueOps.set(cacheKeys.lastClickedAtKey, lastClickedAtStr)
                redisTemplate.expire(cacheKeys.lastClickedAtKey, ttl)
            }

            // Dirty Set
            setOps.add(cacheKeys.dirtySetKey, shortKey)

            null // 반환값 없음
        }
    }

    fun findTotalStatsInCache(shortKey: String): TotalStatsVo? {
        val cacheKeys = TotalStatsCacheKeys.from(shortKey)
        val results = redisTemplate.executePipelined { _ ->
            val valueOps = redisTemplate.opsForValue()
            val hashOps = redisTemplate.opsForHash<String, String>()

            valueOps.get(cacheKeys.totalClicksKey)
            hashOps.entries(cacheKeys.byDateKey)
            hashOps.entries(cacheKeys.byDeviceKey)
            hashOps.entries(cacheKeys.byReferrerKey)
            valueOps.get(cacheKeys.lastClickedAtKey)
            null
        }
        if (!(results.isNotEmpty() && results.size >= 5)) {
            return null
        }
        val (clicksRaw, dateRaw, deviceRaw, referrerRaw, lastClickedRaw) = results
        val totalClicks = (clicksRaw as? String)?.toLongOrNull() ?: return null
        return TotalStatsVo(
            shortKey = shortKey,
            totalClicks = totalClicks,
            byDate = dateRaw.asSafeMap().toDateCountVoList(),
            byDevice = deviceRaw.asSafeMap().toDeviceCountVoList(),
            byReferrer = referrerRaw.asSafeMap().toReferrerCountVoList(),
            lastClickedAt = (lastClickedRaw as? String)?.let { runCatching { Instant.parse(it) }.getOrNull() }
        )
    }
    /**
     * 캐시에서 여러 TotalStatsVo를 한 번에 조회합니다.
     *
     * Redis Pipeline을 사용하여 효율적으로 조회합니다.
     *
     * @param shortKeyList 조회하려는 short Key 리스트
     * @return TotalStatsVo 리스트, 캐시에 없는 경우 해당 항목은 제외됩니다
     */
    fun findTotalStatsInCacheList(shortKeyList: List<String>): List<TotalStatsVo> {
        if (shortKeyList.isEmpty()) {
            return emptyList()
        }
        // 1. Redis Pipeline 실행 (결과는 플랫한 리스트로 반환됨)
        val pipelineResults = redisTemplate.executePipelined { _ ->
            val valueOps = redisTemplate.opsForValue()
            val hashOps = redisTemplate.opsForHash<String, String>()

            shortKeyList.forEach { shortKey ->
                val cacheKeys = TotalStatsCacheKeys.from(shortKey)
                valueOps.get(cacheKeys.totalClicksKey)
                hashOps.entries(cacheKeys.byDateKey)
                hashOps.entries(cacheKeys.byDeviceKey)
                hashOps.entries(cacheKeys.byReferrerKey)
                valueOps.get(cacheKeys.lastClickedAtKey)
            }
            null
        }


        return shortKeyList.zip(pipelineResults.chunked(5))
            .mapNotNull { (shortKey, results) ->
                if (results.size != 5) return@mapNotNull null
                val (clicksRaw, dateRaw, deviceRaw, referrerRaw, lastClickedRaw) = results
                val totalClicks = (clicksRaw as? String)?.toLongOrNull() ?: return@mapNotNull null

                TotalStatsVo(
                    shortKey = shortKey,
                    totalClicks = totalClicks,
                    byDate = dateRaw.asSafeMap().toDateCountVoList(),
                    byDevice = deviceRaw.asSafeMap().toDeviceCountVoList(),
                    byReferrer = referrerRaw.asSafeMap().toReferrerCountVoList(),
                    lastClickedAt = (lastClickedRaw as? String)?.let { runCatching { Instant.parse(it) }.getOrNull() }
                )
            }
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

    /**
     * TotalStats 리스트를 MongoDB에 bulk operations로 저장합니다.
     *
     * @param totalStatsList 저장할 TotalStats 리스트
     */
    fun saveAll(totalStatsList: List<TotalStats>) {
        shortUrlTotalStatsRepository.saveAll(totalStatsList)
    }

    /**
     * dirtySet에서 지정된 개수만큼 shortKey를 꺼냅니다.
     *
     * SPOP 명령어를 사용하여 원자적으로 여러 개를 한 번에 꺼냅니다.
     *
     * @param count 꺼낼 개수
     * @return shortKey 리스트
     */
    fun popDirtyShortKeys(count: Long): List<String> {
        if (count <= 0) return emptyList()
        return redisTemplate.opsForSet().pop(CacheNames.TOTAL_STATS_DIRTY_SET, count)
            ?: emptyList()
    }

    /**
     * Any? 타입을 Map<String, String>으로 안전하게 변환하는 확장 함수
     */
    private fun Any?.asSafeMap(): Map<String, String> {
        return (this as? Map<*, *>)?.entries
            ?.associate { it.key.toString() to it.value.toString() }
            ?: emptyMap()
    }

}
