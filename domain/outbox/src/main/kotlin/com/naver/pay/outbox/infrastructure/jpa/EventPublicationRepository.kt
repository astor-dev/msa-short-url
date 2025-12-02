package com.naver.pay.outbox.infrastructure.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventPublicationRepository : JpaRepository<EventPublicationEntity, Long>
