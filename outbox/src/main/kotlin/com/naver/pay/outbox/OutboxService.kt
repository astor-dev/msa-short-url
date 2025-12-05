package com.naver.pay.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.outbox.infrastructure.jpa.EventPublicationEntity
import com.naver.pay.outbox.infrastructure.jpa.EventPublicationRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class OutboxService(
    private val eventPublicationRepository: EventPublicationRepository,
    private val objectMapper: ObjectMapper,
) {

    fun storeEvent(bindingName: String, event: Any) {
        val message = objectMapper.writeValueAsString(event)
        val eventEntity = EventPublicationEntity(
            bindingName = bindingName,
            message = message,
        )
        eventPublicationRepository.save(eventEntity)
    }

    fun findBatchToProcess(limit: Int): List<EventPublicationEntity> {
        return eventPublicationRepository.findBatchToProcess(limit)
    }

    fun updatePublishedAtByIds(ids: List<Long>, now: Instant): Int {
        return eventPublicationRepository.updatePublishedAtByIds(ids, now)
    }

}