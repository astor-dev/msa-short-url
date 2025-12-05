package com.naver.pay.shorturl.stats.mongodb

import org.springframework.data.mongodb.repository.MongoRepository

interface DailyTopDevicesRepository : MongoRepository<DailyTopDevicesDocument, String> {
    fun findByDateOrderByTotalClicksDesc(date: String): List<DailyTopDevicesDocument>
}