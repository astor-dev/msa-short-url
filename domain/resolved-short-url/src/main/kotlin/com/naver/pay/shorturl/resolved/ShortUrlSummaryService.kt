package com.naver.pay.shorturl.resolved

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ShortUrlSummaryService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    /**
     * 클릭 수 집계 및 최종 클릭 시각 업데이트를 위한 Redis Lua 스크립트
     * KEYS[1]: totalClicksCacheKey
     * KEYS[2]: lastClickedAtCacheKey
     * ARGV[1]: 현재 시간 Instant.now().toString()
     * ARGV[2]: 만료 시간(초)
     */
    private final val clickUpdateScript: RedisScript<String> = DefaultRedisScript(
        """
        local clicks = redis.call('INCR', KEYS[1])
        redis.call('SET', KEYS[2], ARGV[1])
        redis.call('EXPIRE', KEYS[1], ARGV[2])
        redis.call('EXPIRE', KEYS[2], ARGV[2])        
        """,
        String::class.java
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
        executeFullUpdateScript(totalClicksCacheKey, lastClickedAtCacheKey)
    }

    private fun executeFastUpdateScript(totalClicksCacheKey: String, lastClickedAtCacheKey: String) {
        val now = Instant.now().toString()
        val keys = listOf(totalClicksCacheKey, lastClickedAtCacheKey)
        val args = listOf(now)
        redisTemplate.execute(
            clickUpdateScript,
            keys,
            args,
            ttlSeconds
        )
    }

    private fun executeFullUpdateScript(totalClicksCacheKey: String, lastClickedAtCacheKey: String) {
        TODO("Implement full update script execution")
    }
}