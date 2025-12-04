package com.naver.pay.shorturl.resolved

import com.naver.pay.shorturl.stats.ShortUrlTotalStatsService
import com.naver.pay.util.DistributedLockExecutor
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ShortUrlSummaryService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val distributedLockExecutor: DistributedLockExecutor,
    private val shortUrlTotalStatsService: ShortUrlTotalStatsService
) {

    /**
     * 클릭 수 집계 및 최종 클릭 시각 업데이트를 위한 Redis Lua 스크립트
     * KEYS[1]: totalClicksCacheKey
     * KEYS[2]: lastClickedAtCacheKey
     * ARGV[1]: 현재 시간
     * ARGV[2]: 만료 시간(초)
     */
    private final val clickUpdateScript: RedisScript<Void> = DefaultRedisScript(
        """
        redis.call('INCR', KEYS[1])
        redis.call('SET', KEYS[2], ARGV[1])
        redis.call('EXPIRE', KEYS[1], ARGV[2])
        redis.call('EXPIRE', KEYS[2], ARGV[2])        
        """,
        Void::class.java
    )

    /**
     * 클릭 수 캐시를 SET 하기 위한 Redis Lua 스크립트
     * KEYS[1]: totalClicksCacheKey
     * KEYS[2]: lastClickedAtCacheKey
     * ARGV[1]: 설정 할 클릭 수 값
     * ARGV[2]: 현재 시간
     * ARGV[3]: 만료 시간(초)
     */
    private val clickInitScript: RedisScript<Void> = DefaultRedisScript(
        """
        redis.call('SET', KEYS[1], ARGV[1])
        redis.call('SET', KEYS[2], ARGV[2])
        redis.call('EXPIRE', KEYS[1], ARGV[3])
        redis.call('EXPIRE', KEYS[2], ARGV[3])            
        """,
        Void::class.java
    )
    private final val ttlSeconds: Long = 60 * 60 * 24 * 14

    /**
     * 단축 URL 클릭 시 클릭 수 및 최종 클릭 시각을 업데이트합니다.
     * 캐시에 존재하는 경우 빠른 업데이트 스크립트를 사용하고,
     * 캐시에 없는 경우 영속성으로부터 초기 값을 조회해 업데이트 스크립트를 사용합니다.
     * @param shortKey 단축 URL의 고유 키
     */
    fun incrementClickCount(shortKey: String) {
        val totalClicksCacheKey = "${CacheNames.SHORT_URL_TOTAL_CLICKS}::$shortKey"
        val lastClickedAtCacheKey = "${CacheNames.SHORT_URL_LAST_CLICKED_AT}::$shortKey"
        if (redisTemplate.hasKey(totalClicksCacheKey)) {
            executeFastUpdateScript(totalClicksCacheKey, lastClickedAtCacheKey)
            return
        }
        executeFullUpdateScript(shortKey, totalClicksCacheKey, lastClickedAtCacheKey)
    }

    fun findClickSummaryFromPersistence(shortKey: String): ClickSummary {
        return shortUrlTotalStatsService.findOne(shortKey)?.let {
            ClickSummary(
                totalClicks = it.totalClicks,
                lastClickedAt = it.lastClickedAt
            )
        } ?: ClickSummary()

    }

    private fun executeFastUpdateScript(totalClicksCacheKey: String, lastClickedAtCacheKey: String) {
        val now = Instant.now().toString()
        val keys = listOf(totalClicksCacheKey, lastClickedAtCacheKey)
        val args = listOf(now, ttlSeconds)
        redisTemplate.execute(
            clickUpdateScript,
            keys,
            args,
        )
    }

    private fun executeFullUpdateScript(shortKey: String, totalClicksCacheKey: String, lastClickedAtCacheKey: String) {
        val clickSummary = findClickSummaryFromPersistence(shortKey)
        distributedLockExecutor.execute(
            lockName = CacheNames.CLICK_SUMMARY_INIT_LOCK,
            key = shortKey
        ) {
            val initialTotalClicks = clickSummary.totalClicks + 1
            val now = Instant.now().toString()
            val keys = listOf(totalClicksCacheKey, lastClickedAtCacheKey)
            val args = listOf(initialTotalClicks, now, ttlSeconds)
            redisTemplate.execute(
                clickInitScript,
                keys,
                args,
            )
        }

    }
}