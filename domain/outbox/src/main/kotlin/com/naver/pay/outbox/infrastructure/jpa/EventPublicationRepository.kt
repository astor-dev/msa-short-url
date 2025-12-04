package com.naver.pay.outbox.infrastructure.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface EventPublicationRepository : JpaRepository<EventPublicationEntity, Long> {
    @Query(
        value = """
        SELECT id, binding_name , message, created_at , published_at 
        FROM event_publication 
        WHERE published_at IS NULL 
        ORDER BY created_at 
        LIMIT :limit 
        FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findBatchToProcess(limit: Int): List<EventPublicationEntity>

    @Modifying
    @Query("UPDATE EventPublicationEntity e SET e.publishedAt = :now WHERE e.id IN :ids")
    fun updatePublishedAtByIds(ids: List<Long>, now: Instant): Int
}
