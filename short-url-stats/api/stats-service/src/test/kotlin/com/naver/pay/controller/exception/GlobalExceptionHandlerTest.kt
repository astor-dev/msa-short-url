package com.naver.pay.controller.exception

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.URL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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

    data class TestRequestDto(
        @field:NotBlank(message = "name은 필수입니다.")
        val name: String,

        @field:Min(value = 1, message = "age는 최소 1 이상이어야 합니다.")
        val age: Int,

        @field:URL(message = "올바른 URL 형식이어야 합니다.")
        val url: String
    )

    @PostMapping("/test/validation")
    fun testValidation(@Valid @RequestBody requestDto: TestRequestDto) {
        // This endpoint is used to test validation
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

        Given("API 요청 검증 단계에서 예외가 발생했을 때") {
            When("MethodArgumentNotValidException이 발생하면") {
                val result = mockMvc.post("/test/validation") {
                    contentType = org.springframework.http.MediaType.APPLICATION_JSON
                    content = """
                        {
                            "name": "",
                            "age": 0,
                            "url": "invalid-url"
                        }
                    """.trimIndent()
                }

                Then("400 Bad Request와 함께 ErrorResponse를 반환한다") {
                    result.andExpect {
                        status { isBadRequest() }
                        content {
                            contentType("application/json")
                            jsonPath("$.status") { value(HttpStatus.BAD_REQUEST.name) }
                            jsonPath("$.error") { value("ValidationFailed") }
                            jsonPath("$.message") {
                                value(
                                    org.hamcrest.Matchers.containsString("Validation failed for argument(s)")
                                )
                            }
                            jsonPath("$.path") { value("/test/validation") }
                        }
                    }
                }
            }

            When("단일 필드 검증 실패가 발생하면") {
                val result = mockMvc.post("/test/validation") {
                    contentType = org.springframework.http.MediaType.APPLICATION_JSON
                    content = """
                        {
                            "name": "",
                            "age": 10,
                            "url": "https://naver.com"
                        }
                    """.trimIndent()
                }

                Then("해당 필드의 검증 오류 메시지를 포함한 ErrorResponse를 반환한다") {
                    result.andExpect {
                        status { isBadRequest() }
                        content {
                            contentType("application/json")
                            jsonPath("$.status") { value(HttpStatus.BAD_REQUEST.name) }
                            jsonPath("$.error") { value("ValidationFailed") }
                            jsonPath("$.message") {
                                value(
                                    org.hamcrest.Matchers.containsString("name")
                                )
                            }
                            jsonPath("$.path") { value("/test/validation") }
                        }
                    }
                }
            }
        }
    }
}
