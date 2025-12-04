package com.naver.pay.shorturl.resolved.infrastructure.redis

import com.naver.pay.shorturl.resolved.CacheNames
import com.naver.pay.shorturl.resolved.ClickSummary
import com.naver.pay.shorturl.resolved.ResolvedShortUrlCacheService
import com.naver.pay.util.DistributedLockExecutor
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ResolvedShortUrlRedisCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val distributedLockExecutor: DistributedLockExecutor
): ResolvedShortUrlCacheService {

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

    override fun hasKey(key: String): Boolean {
        return redisTemplate.hasKey(key)
    }

    override fun recordClickAtomically(totalClicksCacheKey: String, lastClickedAtCacheKey: String) {
        val now = Instant.now().toString()
        val keys = listOf(totalClicksCacheKey, lastClickedAtCacheKey)
        val args = arrayOf(now, ttlSeconds.toString())
        redisTemplate.execute(
            clickUpdateScript,
            keys,
            *args,
        )
    }

    override fun upsertClick(shortKey: String, totalClicksCacheKey: String, lastClickedAtCacheKey: String, clickSummary: ClickSummary) {
        distributedLockExecutor.execute(
            lockName = CacheNames.CLICK_SUMMARY_INIT_LOCK,
            key = shortKey
        ) {
            val initialTotalClicks = clickSummary.totalClicks + 1
            val now = Instant.now().toString()
            val keys = listOf(totalClicksCacheKey, lastClickedAtCacheKey)
            val args = arrayOf(initialTotalClicks.toString(), now, ttlSeconds.toString())
            redisTemplate.execute(
                clickInitScript,
                keys,
                *args,
            )
        }
    }

    override fun findClickSummary(
        totalClicksCacheKey: String,
        lastClickedAtCacheKey: String
    ): ClickSummary? {
        if (!redisTemplate.hasKey(totalClicksCacheKey)) {
            return null
        }
        val opsForValue = redisTemplate.opsForValue()
        val totalClicks = opsForValue.get(totalClicksCacheKey)?.toLong()
        val lastClickedAt = opsForValue.get(lastClickedAtCacheKey)?.let { Instant.parse(it) }

        return if (totalClicks != null && lastClickedAt != null) {
            ClickSummary(totalClicks, lastClickedAt)
        } else {
            null
        }
    }
}