package com.naver.pay.shorturl.stats.infrastructure.mongodb

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface DailyTopByDeviceUrlsRepository : MongoRepository<DailyTopByDeviceUrlsDocument, String> {
    fun findByDateAndDeviceTypeOrderByRankAsc(
        date: String,
        deviceType: String,
        pageable: Pageable
    ): List<DailyTopByDeviceUrlsDocument>
}