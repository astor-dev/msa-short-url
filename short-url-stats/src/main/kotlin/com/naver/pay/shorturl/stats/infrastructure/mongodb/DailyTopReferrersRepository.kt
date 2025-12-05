package com.naver.pay.shorturl.stats.infrastructure.mongodb

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface DailyTopReferrersRepository : MongoRepository<DailyTopReferrersDocument, String> {
    fun findByDateOrderByRankAsc(date: String, pageable: Pageable): List<DailyTopReferrersDocument>
}