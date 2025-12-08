package com.naver.pay.traffic.config

data class GeneratorConfig(
    val baseUrl: String = "http://localhost:8080",
    val threads: Int = 10,
    val count: Int? = null,
    val durationSeconds: Int? = null,
    val requestIntervalMs: Long = 0,
    val timeoutSeconds: Long = 30L,
    val userAuthToken: String = "test-user-key",
    val adminAuthToken: String = "test-admin-key",
    val userAgent: String = "TrafficGenerator/1.0",
    val referer: String? = null
) {
    init {
        require(threads > 0) { "threads must be greater than 0" }
        require(count == null || count > 0) { "count must be greater than 0 if specified" }
        require(durationSeconds == null || durationSeconds > 0) { "durationSeconds must be greater than 0 if specified" }
        require(count != null || durationSeconds != null) { "either count or durationSeconds must be specified" }
        require(timeoutSeconds > 0L) { "timeoutSeconds must be greater than 0" }
    }
}

