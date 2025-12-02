package com.naver.pay.controller.v1

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.service.ShortUrlService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@ApplyExtension(SpringExtension::class)
@WebMvcTest(UrlController::class)
class UrlControllerTest: BehaviorSpec() {
    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var objectMapper: ObjectMapper
    @MockkBean
    lateinit var shortUrlService: ShortUrlService
    init {
        Given("단축 URL 생성을 위한 API에서") {
            When("유효한 URL로 단축을 요청하면") {
                val originalUrl = "https://naver.com"
                val ttlSeconds = 60
                val requestDto = UrlRequestDto(originalUrl = originalUrl, ttlSeconds = ttlSeconds)
                val urlResponseDto = UrlResponseDto(
                    shortKey = "testKey",
                    shortUrl = "http://localhost/testKey",
                    originalUrl = originalUrl,
                    createdAt = "2025-01-01T00:00:00Z", // Dummy value
                    expiresAt = "2025-01-01T00:00:00Z" // Dummy value
                )
                every { shortUrlService.create(originalUrl, ttlSeconds) } returns urlResponseDto

                val result = mockMvc.post("/v1/urls") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestDto)
                }

                Then("200 OK와 함께 단축 URL 정보를 반환한다") {
                    result.andExpect {
                        status { isOk() }
                        content {
                            contentType(MediaType.APPLICATION_JSON)
                            jsonPath("$.shortKey") { value("testKey") }
                            jsonPath("$.originalUrl") { value(originalUrl) }
                            jsonPath("$.shortUrl") { value("http://localhost/testKey") }
                        }
                    }
                }
            }

            When("잘못된 형식의 URL로 단축을 요청하면") {
                val requestDto = UrlRequestDto(originalUrl = "invalid-url", ttlSeconds = 60)

                val result = mockMvc.post("/v1/urls") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestDto)
                }

                Then("400 Bad Request를 반환한다") {
                    result.andExpect {
                        status { isBadRequest() }
                    }
                }
            }

            When("0 이하의 TTL 값으로 단축을 요청하면") {
                val requestDto = UrlRequestDto(originalUrl = "https://naver.com", ttlSeconds = 0)

                val result = mockMvc.post("/v1/urls") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestDto)
                }

                Then("400 Bad Request를 반환한다") {
                    result.andExpect {
                        status { isBadRequest() }
                    }
                }
            }
        }
    }
}
