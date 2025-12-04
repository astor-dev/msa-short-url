package com.naver.pay.outbox.infrastructure.jpa

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "event_publication",
    indexes = [
        Index(name = "event_publication_by_published_at_idx", columnList = "published_at"),
        Index(name = "event_publication_by_published_at_created_at_idx", columnList = "published_at, created_at")
    ]
)
class EventPublicationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val bindingName: String,
    val message: String,
    val createdAt: Instant = Instant.now(),
    val publishedAt: Instant? = null,
)