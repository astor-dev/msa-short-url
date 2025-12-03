package com.naver.pay.util

import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class DistributedLockExecutor(
    private val redissonClient: RedissonClient
) {
    fun <T> execute(
        lockName: String,
        key: String,
        waitTime: Long = 5,
        leaseTime: Long = 10,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
        block: () -> T
    ): T {
        val rLock = redissonClient.getLock("LOCK::$lockName::$key")
        val available = rLock.tryLock(waitTime, leaseTime, timeUnit)
        if (!available) {
            throw RuntimeException("Lock 획득 실패: $lockName")
        }

        return try {
            block()
        } finally {
            if (rLock.isHeldByCurrentThread) {
                rLock.unlock()
            }
        }
    }
}