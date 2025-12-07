package com.naver.pay.config

import com.naver.pay.processor.TotalStatsItemProcessor
import com.naver.pay.reader.TotalStatsDirtyKeyReader
import com.naver.pay.shorturl.stats.TotalStats
import com.naver.pay.shorturl.stats.TotalStatsRepository
import com.naver.pay.writer.TotalStatsItemWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class TotalStatsSyncJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val totalStatsItemProcessor: TotalStatsItemProcessor,
    private val totalStatsItemWriter: TotalStatsItemWriter,
) {

    private val logger = KotlinLogging.logger(TotalStatsSyncJobConfig::class.java.name)

    @Bean
    fun totalStatsSyncJob(totalStatsSyncStep: Step, totalStatsSyncJobListener: JobExecutionListener): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .listener(totalStatsSyncJobListener)
            .start(totalStatsSyncStep)
            .build()
    }

    @Bean
    fun totalStatsSyncJobListener(): JobExecutionListener {
        return object : JobExecutionListener {
            override fun beforeJob(jobExecution: JobExecution) {
                logger.info { "Starting TotalStats sync job" }
            }

            override fun afterJob(jobExecution: JobExecution) {
                logger.info { 
                    "TotalStats sync job completed. Status: ${jobExecution.status}, " +
                    "Read: ${jobExecution.stepExecutions.sumOf { it.readCount }}, " +
                    "Write: ${jobExecution.stepExecutions.sumOf { it.writeCount }}" 
                }
            }
        }
    }

    @Bean
    fun totalStatsSyncStep(
        totalStatsReader: ItemReader<List<String>>,
        totalStatsProcessor: ItemProcessor<List<String>, List<TotalStats>>,
        totalStatsWriter: ItemWriter<List<TotalStats>>
    ): Step {
        return StepBuilder(STEP_NAME, jobRepository)
            .chunk<List<String>, List<TotalStats>>(CHUNK_SIZE, transactionManager)
            .reader(totalStatsReader)
            .processor(totalStatsProcessor)
            .writer(totalStatsWriter)
            .build()
    }

    @Bean
    @StepScope
    fun totalStatsReader(
        totalStatsRepository: TotalStatsRepository,
        @Value("\${batch.chunk-size:100}") chunkSize: Long
    ): ItemReader<List<String>> {
        return TotalStatsDirtyKeyReader(totalStatsRepository, chunkSize)
    }

    @Bean
    @StepScope
    fun totalStatsProcessor(): ItemProcessor<List<String>, List<TotalStats>> {
        return totalStatsItemProcessor
    }

    @Bean
    @StepScope
    fun totalStatsWriter(): ItemWriter<List<TotalStats>> {
        return totalStatsItemWriter
    }

    companion object {
        private const val JOB_NAME = "totalStatsSyncJob"
        private const val STEP_NAME = "totalStatsSyncStep"
        private const val CHUNK_SIZE = 1
    }
}
