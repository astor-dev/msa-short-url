package com.naver.pay.util

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule


fun getCommonJacksonModules(): List<Module> {
    return listOf(
        JavaTimeModule(),
        KotlinModule.Builder().build()
    )
}

fun createCommonObjectMapper(): ObjectMapper {
    val objectMapper = ObjectMapper()
    getCommonJacksonModules().forEach { module ->
        objectMapper.registerModule(module)
    }
    return objectMapper
}
