package com.naver.pay.shorturl.stats.infrastructure.mongodb

import org.springframework.data.mongodb.repository.MongoRepository

interface ShortUrlTotalStatsRepository : MongoRepository<ShortUrlTotalStatsDocument, String>, ShortUrlTotalStatsCustomRepository