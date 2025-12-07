package com.naver.pay.processor

import com.naver.pay.shorturl.stats.TotalStats
import com.naver.pay.shorturl.stats.TotalStatsService
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class TotalStatsItemProcessor(
    private val totalStatsService: TotalStatsService
) : ItemProcessor<List<String>, List<TotalStats>> {

    override fun process(shortKeyList: List<String>): List<TotalStats> {
        return totalStatsService.findAll(shortKeyList)
    }
}

