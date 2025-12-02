package com.naver.pay.shorturl.infrastructure.jpa

import com.naver.pay.shorturl.ShortUrl
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "short_url")
class ShortUrlEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,
    val shortKey: String?,
    val baseUrl: String,
    val originalUrl: String,
    val expiresAt: Instant,
    @CreationTimestamp
    val createdAt: Instant,
    @UpdateTimestamp
    val updatedAt: Instant,
    val deletedAt: Instant?,
) {
    fun toDomain() : ShortUrl {
        if(id == null) {
            throw RuntimeException("id가 null인 entity는 domain으로 변환할 수 없습니다.")
        }
        return ShortUrl.of(
            id = this.id,
            shortKey = this.shortKey,
            baseUrl = this.baseUrl,
            originalUrl = this.originalUrl,
            createdAt = this.createdAt,
            expiresAt = this.expiresAt,
        )
    }
}