package com.naver.pay.traffic.monitor

import com.naver.pay.traffic.config.GeneratorConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

class ReportGenerator(
    private val scenarioName: String,
    private val outputDir: String = "reports"
) {
    fun generateReport(metrics: RequestMetrics, duration: Duration, config: GeneratorConfig) {
        val jsonReport = generateJsonReport(metrics, duration, config)
        logger.info { "Report generated:\n$jsonReport" }
        saveReportToFile(jsonReport, metrics)
    }

    private fun saveReportToFile(jsonReport: String, metrics: RequestMetrics) {
        try {
            val dir = File(outputDir)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val jsonFile = File(dir, "${scenarioName.lowercase()}_${timestamp}.json")
            jsonFile.writeText(jsonReport)

            logger.info { "JSON report saved to: ${jsonFile.absolutePath}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save report to file" }
        }
    }

    private fun generateJsonReport(metrics: RequestMetrics, duration: Duration, config: GeneratorConfig): String {
        return buildString {
            appendLine("{")
            appendLine("  \"scenario\": \"$scenarioName\",")
            appendLine("  \"duration\": ${duration.inWholeSeconds},")
            appendLine("  \"config\": {")
            appendLine("    \"baseUrl\": \"${config.baseUrl}\",")
            appendLine("    \"threads\": ${config.threads},")
            appendLine("    \"count\": ${config.count ?: "null"},")
            appendLine("    \"durationSeconds\": ${config.durationSeconds ?: "null"},")
            appendLine("    \"requestIntervalMs\": ${config.requestIntervalMs},")
            appendLine("    \"timeoutSeconds\": ${config.timeoutSeconds},")
            appendLine("    \"userAuthToken\": \"${config.userAuthToken}\",")
            appendLine("    \"adminAuthToken\": \"${config.adminAuthToken}\",")
            appendLine("    \"userAgent\": \"${config.userAgent}\",")
            appendLine("    \"referer\": ${if (config.referer != null) "\"${config.referer}\"" else "null"}")
            appendLine("  },")
            appendLine("  \"totalRequests\": ${metrics.totalRequests},")
            appendLine("  \"successfulRequests\": ${metrics.successfulRequests},")
            appendLine("  \"failedRequests\": ${metrics.failedRequests},")
            appendLine("  \"responseTime\": {")
            appendLine("    \"average\": ${metrics.averageResponseTime.inWholeMilliseconds},")
            appendLine("    \"p50\": ${metrics.p50ResponseTime.inWholeMilliseconds},")
            appendLine("    \"p95\": ${metrics.p95ResponseTime.inWholeMilliseconds},")
            appendLine("    \"p99\": ${metrics.p99ResponseTime.inWholeMilliseconds},")
            appendLine("    \"min\": ${metrics.minResponseTime.inWholeMilliseconds},")
            appendLine("    \"max\": ${metrics.maxResponseTime.inWholeMilliseconds}")
            appendLine("  },")
            appendLine("  \"errorBreakdown\": {")
            val errorEntries = metrics.errorBreakdown.entries.joinToString(",\n") { (status, count) ->
                "    \"$status\": $count"
            }
            appendLine(errorEntries)
            appendLine("  },")
            appendLine("  \"firstErrorElapsedTime\": ${if (metrics.firstErrorElapsedTime != null) metrics.firstErrorElapsedTime!!.inWholeMilliseconds else "null"}")
            appendLine("}")
        }
    }
}

