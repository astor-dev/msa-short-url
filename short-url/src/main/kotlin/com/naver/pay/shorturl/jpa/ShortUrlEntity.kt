package com.naver.pay.shorturl.jpa

import com.naver.pay.shorturl.ShortUrl
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "short_url")
class ShortUrlEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var shortKey: String? = null,
    var baseUrl: String,
    var originalUrl: String,
    var expiresAt: Instant,
    @CreationTimestamp
    var createdAt: Instant,
    @UpdateTimestamp
    var updatedAt: Instant,
    var deletedAt: Instant? = null,
) {
    companion object {
        fun of(shortUrl: ShortUrl) : ShortUrlEntity {
            return ShortUrlEntity(
                id = shortUrl.id,
                shortKey = shortUrl.shortKey,
                baseUrl = shortUrl.baseUrl,
                originalUrl = shortUrl.originalUrl,
                expiresAt = shortUrl.expiresAt,
                createdAt = shortUrl.createdAt,
                updatedAt = Instant.now(),
                deletedAt = null,
            )
        }
    }

    fun toDomain() : ShortUrl {
        return id?.let {
            ShortUrl.of(
                id = this.id,
                shortKey = this.shortKey,
                baseUrl = this.baseUrl,
                originalUrl = this.originalUrl,
                createdAt = this.createdAt,
                expiresAt = this.expiresAt,
            )
        } ?: throw RuntimeException("id가 null인 entity는 domain으로 변환할 수 없습니다.")
    }
}