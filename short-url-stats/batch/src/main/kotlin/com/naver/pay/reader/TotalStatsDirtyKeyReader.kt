package com.naver.pay.reader

import com.naver.pay.shorturl.stats.TotalStatsRepository
import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Value

class TotalStatsDirtyKeyReader(
    private val totalStatsRepository: TotalStatsRepository,
    @param:Value("\${batch.chunk-size:100}") private val chunkSize: Long
) : ItemReader<List<String>> {

    private var isFinished = false

    override fun read(): List<String>? {
        if (isFinished) {
            return null
        }

        val shortKeyList = totalStatsRepository.popDirtyShortKeys(chunkSize)
        
        if (shortKeyList.isEmpty()) {
            isFinished = true
            return null
        }

        return shortKeyList
    }
}

