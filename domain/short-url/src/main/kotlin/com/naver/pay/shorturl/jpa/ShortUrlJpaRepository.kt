package com.naver.pay.shorturl.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ShortUrlJpaRepository : JpaRepository<ShortUrlEntity, Long> {
    fun findByOriginalUrl(originalUrl: String): Optional<ShortUrlEntity>
    fun findByShortKey(shortKey: String): Optional<ShortUrlEntity>
}