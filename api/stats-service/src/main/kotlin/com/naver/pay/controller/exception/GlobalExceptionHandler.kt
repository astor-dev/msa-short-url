package com.naver.pay.controller.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNoSuchElementException(e: NoSuchElementException, request: WebRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity(
            ErrorResponse(
                timestamp = System.currentTimeMillis().toString(),
                status = HttpStatus.NOT_FOUND,
                error = e.javaClass.simpleName,
                message = e.message ?: "The requested resource was not found.",
                path = request.getDescription(false).substringAfter("uri=")
            ),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(e: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity(
            ErrorResponse(
                timestamp = System.currentTimeMillis().toString(),
                status = HttpStatus.BAD_REQUEST,
                error = e.javaClass.simpleName,
                message = e.message ?: "The request was invalid.",
                path = request.getDescription(false).substringAfter("uri=")
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(e: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity(
            ErrorResponse(
                timestamp = System.currentTimeMillis().toString(),
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                error = e.javaClass.simpleName,
                message = e.message ?: "An unexpected error occurred.",
                path = request.getDescription(false).substringAfter("uri=")
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}