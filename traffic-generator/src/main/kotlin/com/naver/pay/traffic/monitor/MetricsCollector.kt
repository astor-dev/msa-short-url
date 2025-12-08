package com.naver.pay.traffic.monitor

import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class RequestMetrics(
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTime: Duration,
    val p50ResponseTime: Duration,
    val p95ResponseTime: Duration,
    val p99ResponseTime: Duration,
    val maxResponseTime: Duration,
    val minResponseTime: Duration,
    val errorBreakdown: Map<HttpStatusCode, Long>,
    val firstErrorElapsedTime: Duration?
)

class MetricsCollector {
    private val mutex = Mutex()
    private val totalRequests = AtomicLong(0)
    private val successfulRequests = AtomicLong(0)
    private val failedRequests = AtomicLong(0)
    private val responseTimes = mutableListOf<Long>()
    private val errorBreakdown = mutableMapOf<HttpStatusCode, AtomicLong>()
    private var startTimeMillis: Long? = null
    private var firstErrorElapsedTime: Duration? = null

    fun setStartTime(startTimeMillis: Long) {
        this.startTimeMillis = startTimeMillis
    }

    suspend fun recordRequest(
        success: Boolean,
        responseTime: Duration,
        statusCode: HttpStatusCode? = null
    ) {
        mutex.withLock {
            totalRequests.incrementAndGet()
            if (success) {
                successfulRequests.incrementAndGet()
            } else {
                failedRequests.incrementAndGet()
                if (firstErrorElapsedTime == null && startTimeMillis != null) {
                    val currentTimeMillis = System.currentTimeMillis()
                    val elapsedMillis = currentTimeMillis - startTimeMillis!!
                    firstErrorElapsedTime = elapsedMillis.milliseconds
                }
                statusCode?.let {
                    errorBreakdown.getOrPut(it) { AtomicLong(0) }.incrementAndGet()
                }
            }
            responseTimes.add(responseTime.inWholeMilliseconds)
        }
    }

    suspend fun getMetrics(): RequestMetrics {
        return mutex.withLock {
            val sortedTimes = responseTimes.sorted()
            val count = sortedTimes.size

            if (count == 0) {
                return RequestMetrics(
                    totalRequests = 0,
                    successfulRequests = 0,
                    failedRequests = 0,
                    averageResponseTime = Duration.ZERO,
                    p50ResponseTime = Duration.ZERO,
                    p95ResponseTime = Duration.ZERO,
                    p99ResponseTime = Duration.ZERO,
                    maxResponseTime = Duration.ZERO,
                    minResponseTime = Duration.ZERO,
                    errorBreakdown = emptyMap(),
                    firstErrorElapsedTime = null
                )
            }

            val average = sortedTimes.average().milliseconds
            val p50 = sortedTimes[(count * 0.5).toInt()].milliseconds
            val p95 = sortedTimes[(count * 0.95).toInt()].milliseconds
            val p99 = sortedTimes[(count * 0.99).toInt()].milliseconds
            val max = sortedTimes.last().milliseconds
            val min = sortedTimes.first().milliseconds

            RequestMetrics(
                totalRequests = totalRequests.get(),
                successfulRequests = successfulRequests.get(),
                failedRequests = failedRequests.get(),
                averageResponseTime = average,
                p50ResponseTime = p50,
                p95ResponseTime = p95,
                p99ResponseTime = p99,
                maxResponseTime = max,
                minResponseTime = min,
                errorBreakdown = errorBreakdown.mapValues { it.value.get() },
                firstErrorElapsedTime = firstErrorElapsedTime
            )
        }
    }

    suspend fun reset() {
        mutex.withLock {
            totalRequests.set(0)
            successfulRequests.set(0)
            failedRequests.set(0)
            responseTimes.clear()
            errorBreakdown.clear()
            startTimeMillis = null
            firstErrorElapsedTime = null
        }
    }
}

