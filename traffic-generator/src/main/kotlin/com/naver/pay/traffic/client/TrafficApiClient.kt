package com.naver.pay.traffic.client

import com.naver.pay.traffic.client.model.*
import com.naver.pay.traffic.config.GeneratorConfig
import com.naver.pay.traffic.monitor.MetricsCollector
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class TrafficApiClient(
    private val config: GeneratorConfig,
    private val metricsCollector: MetricsCollector,
    private val enableRedirects: Boolean = false
) {
    private val client = HttpClient(CIO) {
        expectSuccess = false // Handle all responses manually
        followRedirects = this@TrafficApiClient.enableRedirects

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
        install(DefaultRequest) {
            url(config.baseUrl)
        }
        engine {
            requestTimeout = config.timeoutSeconds.seconds.inWholeMilliseconds
        }
    }

    // --- Scenario Methods ---

    suspend fun createShortUrl(): CreateUrlResponse? {
        val originalUrl = "https://www.example.com/long-url-${UUID.randomUUID()}"
        val request = CreateUrlRequest(originalUrl = originalUrl, ttlSeconds = 2592000)
        val headers = mapOf("Authorization" to "Bearer ${config.userAuthToken}")

        logger.info { "Attempting to create short URL for: $originalUrl" }

        val result = executeRequest<CreateUrlRequest, CreateUrlResponse>(
            url = "/api/v1/urls",
            method = HttpMethod.Post,
            requestBody = request,
            additionalHeaders = headers
        )

        return result.fold(
            onSuccess = { response ->
                logger.info { "Successfully created short URL: ${response.shortUrl}" }
                response
            },
            onFailure = { e ->
                logger.error(e) { "Failed to create short URL for $originalUrl" }
                null
            }
        )
    }

    suspend fun checkRedirect(shortKey: String): Boolean {
        val url = "/$shortKey"
        val headers = mutableMapOf("User-Agent" to config.userAgent)
        config.referer?.let { headers["Referer"] = it }

        logger.info { "Attempting to redirect short URL: $shortKey" }

        val result = executeRequest<Unit, Unit>(
            url = url,
            method = HttpMethod.Get,
            additionalHeaders = headers
        )

        return result.isSuccess
    }

    suspend fun getUrlState(shortKey: String): UrlStateResponse? {
        val url = "/api/v1/urls/$shortKey"
        val headers = mapOf("Authorization" to "Bearer ${config.userAuthToken}")

        logger.info { "Attempting to retrieve state for short URL: $shortKey" }

        val result = executeRequest<Unit, UrlStateResponse>(
            url = url,
            method = HttpMethod.Get,
            additionalHeaders = headers
        )

        return result.fold(
            onSuccess = { response ->
                logger.info { "Successfully retrieved state for $shortKey. Total Clicks: ${response.clickSummary.totalClicks}" }
                response
            },
            onFailure = { e ->
                logger.error(e) { "Failed to retrieve state for short URL: $shortKey" }
                null
            }
        )
    }

    suspend fun getDetailStatistics(shortKey: String): StatisticsResponse? {
        val url = "/api/v1/urls/$shortKey/statistics"
        val headers = mapOf("Authorization" to "Bearer ${config.adminAuthToken}")

        logger.info { "Attempting to retrieve detailed statistics for short URL: $shortKey" }

        val result = executeRequest<Unit, StatisticsResponse>(
            url = url,
            method = HttpMethod.Get,
            additionalHeaders = headers
        )

        return result.fold(
            onSuccess = { response ->
                logger.info { "Successfully retrieved detailed statistics for $shortKey. Total Clicks: ${response.totalClicks}" }
                response
            },
            onFailure = { e ->
                logger.error(e) { "Failed to retrieve detailed statistics for short URL: $shortKey" }
                null
            }
        )
    }

    suspend fun getTopNStatistics(
        date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
        limit: Int? = null
    ): TopStatisticsResponse? {
        val url = "/api/v1/statistics/top"
        val headers = mapOf("Authorization" to "Bearer ${config.adminAuthToken}")
        val queryParams = mutableMapOf("date" to date)
        limit?.let { queryParams["limit"] = it.toString() }

        logger.info { "Attempting to retrieve Top N statistics for date: $date with limit: ${limit ?: "default"}" }

        val result = executeRequest<Unit, TopStatisticsResponse>(
            url = url,
            method = HttpMethod.Get,
            additionalHeaders = headers,
            queryParameters = queryParams
        )

        return result.fold(
            onSuccess = { response ->
                logger.info { "Successfully retrieved Top N statistics for date: ${response.date}. Top URLs count: ${response.topUrls.size}" }
                response
            },
            onFailure = { e ->
                logger.error(e) { "Failed to retrieve Top N statistics for date: $date" }
                null
            }
        )
    }

    private suspend inline fun <reified REQUEST : Any, reified RESPONSE : Any> executeRequest(
        url: String,
        method: HttpMethod,
        requestBody: REQUEST? = null,
        additionalHeaders: Map<String, String> = emptyMap(),
        queryParameters: Map<String, String> = emptyMap()
    ): Result<RESPONSE> {
        val startTime = System.nanoTime()
        return runCatching {
            val fullUrl = if (url.startsWith("http")) url else "${config.baseUrl}$url"

            logger.info {
                "[API REQUEST] ${method.value} $fullUrl | Body: ${
                    requestBody?.toString()?.take(100) ?: "N/A"
                }"
            }

            val response = client.request(fullUrl) {
                this.method = method
                additionalHeaders.forEach { (key, value) -> header(key, value) }
                queryParameters.forEach { (key, value) -> parameter(key, value) }
                if (requestBody != null) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
            }

            val endTime = System.nanoTime()
            val responseTime = (endTime - startTime).nanoseconds

            if (response.status.value in 200..399) {
                val responseBody = response.body<RESPONSE>()
                logger.info { "[API RESPONSE] ${response.status} | Body: ${responseBody.toString().take(200)}" }
                metricsCollector.recordRequest(true, responseTime, response.status) // Record success
                responseBody
            } else {
                val errorBody = response.body<String>()
                val errorMessage = "API Request failed with status ${response.status}: $errorBody"
                logger.error { "[API ERROR] $errorMessage" }
                metricsCollector.recordRequest(false, responseTime, response.status) // Record failure
                throw IllegalStateException(errorMessage)
            }
        }.onFailure { e ->
            val endTime = System.nanoTime()
            val responseTime = (endTime - startTime).nanoseconds
            metricsCollector.recordRequest(
                false,
                responseTime
            ) // Record failure (status code might not be available)
        }
    }

    fun close() {
        client.close()
    }
}
