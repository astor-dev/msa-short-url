package com.naver.pay.config

import com.naver.pay.shorturl.stats.DailyTopStatsService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.ZoneId

@Configuration
class DailyTopStatsSyncJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val shortUrlDailyTopStatsService: DailyTopStatsService,
) {

    private val logger = KotlinLogging.logger(DailyTopStatsSyncJobConfig::class.java.name)

    // TODO: 날짜별 격리 트랜잭션(Step 2개로 분리)
    // TODO: Chunk 기반 전환
    // TODO: Multi-thread Step 사용
    // TODO: 중간 실패 시 재시작 Chunk 기반 Writer 적용
    // TODO: Redis 데이터 읽을 때 Cursor Reader 도입
    @Bean
    fun dailyTopStatsSyncJob(dailyTopStatsSyncStep: Step): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .start(dailyTopStatsSyncStep)
            .build()
    }

    @Bean
    fun dailyTopStatsSyncStep(dailyTopStatsSyncTasklet: Tasklet): Step {
        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(dailyTopStatsSyncTasklet, transactionManager)
            .build()
    }

    @Bean
    @StepScope
    fun dailyTopStatsSyncTasklet(
        @Value("#{jobParameters['limit'] ?: 100}") limit: Long
    ): Tasklet {
        return Tasklet { _, _ ->
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val yesterday = today.minusDays(1)

            listOf(yesterday, today).forEach { date ->
                val dailyTopStats = shortUrlDailyTopStatsService.getOne(date, limit)
                if (dailyTopStats.topUrls.isNotEmpty()) shortUrlDailyTopStatsService.save(dailyTopStats)
                logger.info { "${"Synced daily top stats for date={}, limit={}"} $date $limit" }
            }

            RepeatStatus.FINISHED
        }
    }

    companion object {
        private const val JOB_NAME = "dailyTopStatsSyncJob"
        private const val STEP_NAME = "dailyTopStatsSyncStep"
    }
}

