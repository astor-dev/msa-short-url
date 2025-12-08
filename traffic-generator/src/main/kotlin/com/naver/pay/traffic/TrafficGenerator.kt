package com.naver.pay.traffic

import com.naver.pay.traffic.client.TrafficApiClient
import com.naver.pay.traffic.config.GeneratorConfig
import com.naver.pay.traffic.monitor.MetricsCollector
import com.naver.pay.traffic.monitor.ReportGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val logger = KotlinLogging.logger {}

private val TEST_SHORT_KEYS = listOf("MQ", "Mg", "Mw", "NA", "NQ", "Ng")

fun main() = runBlocking {
    TrafficGenerator().run()
}

enum class ScenarioType(val displayName: String, val description: String) {
    CREATE("URL 생성", "URL 생성 API 트래픽 생성"),
    REDIRECT("리다이렉트", "리다이렉트 API 트래픽 생성"),
    STATE("상태 조회", "URL 상태 조회 API 트래픽 생성"),
    STATISTICS("통계", "상세 통계 API 트래픽 생성"),
    TOP_N("상위 N개", "상위 N개 통계 API 트래픽 생성"),
}

class TrafficGenerator {
    private val reader = BufferedReader(InputStreamReader(System.`in`))
    private var config = GeneratorConfig(
        // Changed to var
        baseUrl = "http://localhost:8080",
        threads = 10,
        count = 100,
        durationSeconds = null,
        requestIntervalMs = 0L,
        timeoutSeconds = 30L,
        userAuthToken = "test-user-key",
        adminAuthToken = "test-admin-key",
        userAgent = "TrafficGenerator/1.0",
        referer = null,
    )
    
    private fun readInput(): String? {
        return try {
            reader.readLine()
        } catch (_: Exception) {
            null
        }
    }

    private fun promptInput(promptText: String, default: String? = null): String {
        val defaultText = if (default != null) " [$default]" else ""
        print("$promptText$defaultText: ")
        System.out.flush()
        val input = readInput()?.trim() ?: ""
        return if (input.isBlank() && default != null) default else input
    }

    private fun promptInt(promptText: String, default: Int? = null, validator: (Int) -> Boolean = { it > 0 }): Int {
        while (true) {
            val defaultText = if (default != null) " [$default]" else ""
            print("$promptText$defaultText: ")
            System.out.flush()
            val input = readInput()?.trim() ?: ""
            val value = if (input.isBlank() && default != null) {
                default
            } else {
                input.toIntOrNull()
            }
            if (value != null && validator(value)) {
                return value
            }
            println("잘못된 입력입니다. 올바른 숫자를 입력해주세요.")
        }
    }

    private fun promptLong(promptText: String, default: Long? = null, validator: (Long) -> Boolean = { it >= 0 }): Long {
        while (true) {
            val defaultText = if (default != null) " [$default]" else ""
            print("$promptText$defaultText: ")
            System.out.flush()
            val input = readInput()?.trim() ?: ""
            val value = if (input.isBlank() && default != null) {
                default
            } else {
                input.toLongOrNull()
            }
            if (value != null && validator(value)) {
                return value
            }
            println("잘못된 입력입니다. 올바른 숫자를 입력해주세요.")
        }
    }

    private fun promptYesNo(promptText: String, default: Boolean = false): Boolean {
        while (true) {
            val defaultText = if (default) " [Y/n]" else " [y/N]"
            print("$promptText$defaultText: ")
            System.out.flush()
            val input = readInput()?.trim()?.lowercase() ?: ""
            val value = when {
                input.isBlank() -> default
                input in listOf("y", "yes", "ㅛ") -> true
                input in listOf("n", "no", "ㅜ") -> false
                else -> null
            }
            if (value != null) {
                return value
            }
            println("잘못된 입력입니다. y 또는 n을 입력해주세요.")
        }
    }

    private fun showMainMenu(): Int {
        println()
        println("═══════════════════════════════════════════════════════════")
        println("메인 메뉴:")
        println("  1. 시나리오 선택 및 실행")
        println("  2. 설정 변경")
        println("  3. 종료")
        println("═══════════════════════════════════════════════════════════")
        return promptInt("선택 (1-3)", 1) { it in 1..3 }
    }

    private fun showConfigMenu() {
        val executionMode = if (config.count != null) 1 else 2
        
        println()
        println("═══════════════════════════════════════════════════════════")
        println("설정 변경:")
        println("  1. 기본 URL: ${config.baseUrl}")
        println("  2. 동시 실행 스레드 수: ${config.threads}")
        println("  3. 실행 모드: ${if (executionMode == 1) "요청 수 기준" else "시간 기준"}")
        if (executionMode == 1) {
            println("  4. 총 요청 수: ${config.count ?: "미설정"}")
        } else {
            println("  4. 실행 시간 (초): ${config.durationSeconds ?: "미설정"}")
        }
        println("  5. 요청 간격 (밀리초): ${config.requestIntervalMs}")
        println("  6. 요청 타임아웃 (초): ${config.timeoutSeconds}")
        println("  7. 사용자 인증 토큰: ${config.userAuthToken}")
        println("  8. 관리자 인증 토큰: ${config.adminAuthToken}")
        println("  9. User-Agent 헤더: ${config.userAgent}")
        println("  10. Referer 헤더: ${config.referer ?: "미설정"}")
        println("  11. 뒤로 가기")
        println("═══════════════════════════════════════════════════════════")
        
        val choice = promptInt("변경할 설정 선택 (1-11)", 11) { it in 1..11 }
        
        when (choice) {
            1 -> {
                val baseUrl = promptInput("기본 URL", config.baseUrl)
                config = config.copy(baseUrl = baseUrl)
            }
            2 -> {
                val threads = promptInt("동시 실행 스레드 수", config.threads) { it > 0 }
                config = config.copy(threads = threads)
            }
            3 -> {
                println("실행 모드:")
                println("  1. 요청 수 기준 (총 요청 수)")
                println("  2. 시간 기준 (실행 시간(초))")
                val mode = promptInt("모드 선택 (1 또는 2)", executionMode) { it == 1 || it == 2 }
                if (mode == 1) {
                    val count = promptInt("총 요청 수", config.count ?: 100) { it > 0 }
                    config = config.copy(count = count, durationSeconds = null)
                } else {
                    val duration = promptInt("실행 시간 (초)", config.durationSeconds ?: 60) { it > 0 }
                    config = config.copy(count = null, durationSeconds = duration)
                }
            }
            4 -> {
                if (executionMode == 1) {
                    val count = promptInt("총 요청 수", config.count ?: 100) { it > 0 }
                    config = config.copy(count = count)
                } else {
                    val duration = promptInt("실행 시간 (초)", config.durationSeconds ?: 60) { it > 0 }
                    config = config.copy(durationSeconds = duration)
                }
            }
            5 -> {
                val interval = promptLong("요청 간격 (밀리초)", config.requestIntervalMs) { it >= 0 }
                config = config.copy(requestIntervalMs = interval)
            }
            6 -> {
                val timeout = promptLong("요청 타임아웃 (초)", config.timeoutSeconds) { it > 0 }
                config = config.copy(timeoutSeconds = timeout)
            }
            7 -> {
                val userToken = promptInput("사용자 인증 토큰", config.userAuthToken)
                config = config.copy(userAuthToken = userToken)
            }
            8 -> {
                val adminToken = promptInput("관리자 인증 토큰", config.adminAuthToken)
                config = config.copy(adminAuthToken = adminToken)
            }
            9 -> {
                val userAgent = promptInput("User-Agent 헤더", config.userAgent)
                config = config.copy(userAgent = userAgent)
            }
            10 -> {
                val referer = promptInput("Referer 헤더 (선택사항, Enter로 건너뛰기)")
                config = config.copy(referer = referer.takeIf { it.isNotBlank() })
            }
            11 -> return
        }
        
        println("설정이 변경되었습니다.")
        showConfigMenu()
    }

    private suspend fun selectAndRunScenario() {
        println()
        println("사용 가능한 시나리오:")
        ScenarioType.entries.forEachIndexed { index, scenario ->
            println("  ${index + 1}. ${scenario.displayName} - ${scenario.description}")
        }
        println()

        val scenarioChoice = promptInt("시나리오 선택 (1-${ScenarioType.entries.size})") {
            it in 1..ScenarioType.entries.size
        }
        val selectedScenario = ScenarioType.entries[scenarioChoice - 1]
        println("선택됨: ${selectedScenario.displayName}")
        println()

        // 시나리오별 추가 설정 확인
        val currentConfig = config

        // 설정 요약 출력
        println()
        println("═══════════════════════════════════════════════════════════")
        println("설정 요약:")
        println("  시나리오: ${selectedScenario.displayName}")
        println("  기본 URL: ${currentConfig.baseUrl}")
        println("  스레드 수: ${currentConfig.threads}")
        if (currentConfig.count != null) {
            println("  총 요청 수: ${currentConfig.count}")
        } else {
            println("  실행 시간: ${currentConfig.durationSeconds}초")
        }
        println("  요청 간격: ${currentConfig.requestIntervalMs}ms")
        println("  타임아웃: ${currentConfig.timeoutSeconds}초")
        println("  사용자 토큰: ${currentConfig.userAuthToken}")
        println("  관리자 토큰: ${currentConfig.adminAuthToken}")
        // Short key file is no longer hardcoded
        println("═══════════════════════════════════════════════════════════")
        println()

        val confirm = promptYesNo("트래픽을 시작하시겠습니까?", true)

        if (!confirm) {
            println("취소되었습니다.")
            return
        }

        // Initialize MetricsCollector and ReportGenerator
        val metricsCollector = MetricsCollector()
        val reportGenerator = ReportGenerator(selectedScenario.name)

        // Determine if redirects should be enabled for the TrafficApiClient
        val enableRedirects = (selectedScenario != ScenarioType.REDIRECT)

        // Create the single TrafficApiClient instance
        val trafficApiClient = TrafficApiClient(
            config = currentConfig,
            metricsCollector = metricsCollector,
            enableRedirects = enableRedirects
        )

        val totalExecutionTime = measureTimeMillis {
            coroutineScope {
                val jobs = mutableListOf<Job>()
                val requestCounter = AtomicLong(0)
                val keyIndex = AtomicInteger(0)
                val startTime = System.currentTimeMillis()
                metricsCollector.setStartTime(startTime)

                repeat(currentConfig.threads) { threadId ->
                    jobs.add(
                        launch(Dispatchers.IO) {
                            while (isActive) { // Keep running until scope is cancelled or condition met
                                // Check duration-based termination
                                if (currentConfig.durationSeconds != null && (System.currentTimeMillis() - startTime) >= currentConfig.durationSeconds * 1000) {
                                    break
                                }

                                // Atomically increment counter and check if we've exceeded the limit
                                // This ensures exactly 'count' requests are executed
                                val currentCount = requestCounter.getAndIncrement()
                                if (currentConfig.count != null && currentCount >= currentConfig.count) {
                                    break
                                }

                                try {
                                    when (selectedScenario) {
                                        ScenarioType.CREATE -> trafficApiClient.createShortUrl()
                                        ScenarioType.REDIRECT -> {
                                            val shortKey = TEST_SHORT_KEYS[keyIndex.getAndIncrement() % TEST_SHORT_KEYS.size]
                                            trafficApiClient.checkRedirect(shortKey)
                                        }
                                        ScenarioType.STATE -> {
                                            val shortKey = TEST_SHORT_KEYS[keyIndex.getAndIncrement() % TEST_SHORT_KEYS.size]
                                            trafficApiClient.getUrlState(shortKey)
                                        }
                                        ScenarioType.STATISTICS -> {
                                            val shortKey = TEST_SHORT_KEYS[keyIndex.getAndIncrement() % TEST_SHORT_KEYS.size]
                                            trafficApiClient.getDetailStatistics(shortKey)
                                        }
                                        ScenarioType.TOP_N -> trafficApiClient.getTopNStatistics()
                                    }
                                } catch (e: Exception) {
                                    logger.error(e) { "Error in scenario execution for thread $threadId" }
                                }

                                if (currentConfig.requestIntervalMs > 0) {
                                    delay(currentConfig.requestIntervalMs)
                                }
                            }
                        }
                    )
                }
                jobs.joinAll() // Wait for all threads to complete
            }
        }
        trafficApiClient.close() // Close client after all jobs are done

        // Generate report
        val finalMetrics = metricsCollector.getMetrics()
        reportGenerator.generateReport(finalMetrics, totalExecutionTime.toDuration(DurationUnit.MILLISECONDS), currentConfig)
        metricsCollector.reset() // Reset metrics for next run
    }

    suspend fun run() {
        println("╔════════════════════════════════════════════════════════════╗")
        println("║   단축 URL 플랫폼 트래픽 생성기                            ║")
        println("╚════════════════════════════════════════════════════════════╝")

        while (true) {
            when (showMainMenu()) {
                1 -> selectAndRunScenario()
                2 -> showConfigMenu()
                3 -> {
                    println("종료합니다.")
                    break
                }
            }
        }
    }
}
