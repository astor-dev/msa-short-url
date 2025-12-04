package com.naver.pay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ShortUrlStatsProcessorApplication

fun main(args: Array<String>) {
    runApplication<ShortUrlStatsProcessorApplication>(*args)
}