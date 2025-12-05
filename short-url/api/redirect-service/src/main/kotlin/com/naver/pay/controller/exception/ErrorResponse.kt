package com.naver.pay.controller.exception

import org.springframework.http.HttpStatus

data class ErrorResponse (
    val timestamp: String,
    val status: HttpStatus,
    val error: String,
    val message: String,
    val path: String
)