package com.naver.pay.writer

import com.naver.pay.shorturl.stats.TotalStats
import com.naver.pay.shorturl.stats.TotalStatsService
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class TotalStatsItemWriter(
    private val totalStatsService: TotalStatsService
) : ItemWriter<List<TotalStats>> {

    override fun write(chunk: Chunk<out List<TotalStats>>) {
        if (chunk.items.isEmpty()) {
            return
        }

        val allTotalStatsList = chunk.items.flatMap { it }
        if (allTotalStatsList.isNotEmpty()) {
            totalStatsService.saveAll(allTotalStatsList)
        }
    }
}

