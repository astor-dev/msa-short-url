package com.naver.pay.outbox

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Component
class OutboxPollingPublisherScheduler(
    private val outboxService: OutboxService,
    private val streamBridge: StreamBridge,
    private val transactionTemplate: TransactionTemplate
) {

    companion object {
        private const val BATCH_SIZE = 50
    }

    val logger = KotlinLogging.logger(OutboxPollingPublisherScheduler::class.java.name)

    // NOTE: 0.5초 주기
    @Scheduled(fixedDelay = 500)
    fun pollAndPublish() {
        // DB 읽기 (SKIP LOCKED로 내 몫만 가져오기)
        val events = transactionTemplate.execute {
            outboxService.findBatchToProcess(BATCH_SIZE)
        } ?: return

        if (events.isEmpty()) return

        val successIds = runBlocking(Dispatchers.IO) {
            events.map { event ->
                async {
                    try {
                        val sent = streamBridge.send(event.bindingName, event.message)
                        if (sent) event.id else null
                    } catch (e: Exception) {
                         logger.error(e) {"failed to publish event: ${event.id}" }
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        // NOTE: at least once는 보장하나 중복 발행의 여지가 있어 멱등 컨슈머 필요
        if (successIds.isNotEmpty()) {
            transactionTemplate.execute {
                outboxService.updatePublishedAtByIds(successIds, Instant.now())
            }
        }
    }
}