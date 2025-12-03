package com.naver.pay.outbox.infrastructure.jpa

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "event_publication")
class EventPublicationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val bindingName: String,
    val message: String,
    val createdAt: Instant = Instant.now(),
    val publishedAt: Instant? = null,
)