package com.naver.pay.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.outbox.infrastructure.jpa.EventPublicationEntity
import com.naver.pay.outbox.infrastructure.jpa.EventPublicationRepository
import org.springframework.stereotype.Service

@Service
class OutboxService(
    private val eventPublicationRepository: EventPublicationRepository,
    private val objectMapper: ObjectMapper,
) {

    fun publishEvent(destination: String, event: Any) {
        val message = objectMapper.writeValueAsString(event)
        val eventEntity = EventPublicationEntity(
            destination = destination,
            message = message,
        )
        eventPublicationRepository.save(eventEntity)
    }
}