package com.naver.pay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class UrlServiceApplication

fun main(args: Array<String>) {
    runApplication<UrlServiceApplication>(*args)
}