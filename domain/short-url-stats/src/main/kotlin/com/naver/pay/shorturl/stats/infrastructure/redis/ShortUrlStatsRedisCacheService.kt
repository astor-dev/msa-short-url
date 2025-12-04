package com.naver.pay.shorturl.stats.infrastructure.redis

import com.naver.pay.shorturl.stats.CacheNames
import com.naver.pay.shorturl.stats.ShortUrlStatsCacheService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class ShortUrlStatsRedisCacheService(
    private val redisTemplate: RedisTemplate<String, String>
): ShortUrlStatsCacheService {
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

    override fun recordClickAtomically(
        dateKey: String,
        shortKey: String,
        referrer: String,
        device: String
    ) {
        val globalUrlKey = "${CacheNames.DAILY_TOP_URLS}::$dateKey"
        val referrerKey = "${CacheNames.DAILY_TOP_REFERRERS}::$dateKey"
        val deviceParentKey = "${CacheNames.DAILY_TOP_DEVICES}::$dateKey"
        val deviceChildKey = "${CacheNames.DAILY_TOP_DEVICES}::$dateKey::${CacheNames.INFIX_DAILY_TOP_URLS}::$device"

        val keys = listOf(globalUrlKey, referrerKey, deviceParentKey, deviceChildKey)
        val args = arrayOf(shortKey, referrer, device, ttlSeconds.toString())

        redisTemplate.execute(
            recordStatsScript,
            keys,
            *args,
        )
    }
}