package com.naver.pay.controller.exception

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.java

@RestController
class TestController {
    @GetMapping("/test/not-found")
    fun throwNoSuchElement() {
        throw NoSuchElementException("Test Not Found")
    }

    @GetMapping("/test/bad-request")
    fun throwIllegalArgumentException() {
        throw IllegalArgumentException("Test Bad Request")
    }

    @GetMapping("/test/internal-error")
    fun throwGenericException() {
        throw Exception("Test Internal Server Error")
    }
}


@ApplyExtension(SpringExtension::class)
@WebMvcTest(controllers = [TestController::class])
class GlobalExceptionHandlerTest: BehaviorSpec() {
    @Autowired
    lateinit var mockMvc: MockMvc

    init {
        Given("API 요청 처리 중 예외가 발생했을 때") {
            When("NoSuchElementException이 발생하면") {
                val result = mockMvc.get("/test/not-found")

                Then("404 Not Found와 함께 ErrorResponse를 반환한다") {
                    result.andExpect {
                        status { isNotFound() }
                        content {
                            contentType("application/json")
                            jsonPath("$.status") { value(HttpStatus.NOT_FOUND.name) }
                            jsonPath("$.error") { value(NoSuchElementException::class.java.simpleName) }
                            jsonPath("$.message") { value("Test Not Found") }
                            jsonPath("$.path") { value("/test/not-found") }
                        }
                    }
                }
            }

            When("IllegalArgumentException이 발생하면") {
                val result = mockMvc.get("/test/bad-request")

                Then("400 Bad Request와 함께 ErrorResponse를 반환한다") {
                    result.andExpect {
                        status { isBadRequest() }
                        content {
                            contentType("application/json")
                            jsonPath("$.status") { value(HttpStatus.BAD_REQUEST.name) }
                            jsonPath("$.error") { value(IllegalArgumentException::class.java.simpleName) }
                            jsonPath("$.message") { value("Test Bad Request") }
                            jsonPath("$.path") { value("/test/bad-request") }
                        }
                    }
                }
            }

            When("일반적인 Exception이 발생하면") {
                val result = mockMvc.get("/test/internal-error")

                Then("500 Internal Server Error와 함께 ErrorResponse를 반환한다") {
                    result.andExpect {
                        status { isInternalServerError() }
                        content {
                            contentType("application/json")
                            jsonPath("$.status") { value(HttpStatus.INTERNAL_SERVER_ERROR.name) }
                            jsonPath("$.error") { value(Exception::class.java.simpleName) }
                            jsonPath("$.message") { value("Test Internal Server Error") }
                            jsonPath("$.path") { value("/test/internal-error") }
                        }
                    }
                }
            }
        }
    }
}
