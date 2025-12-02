package com.naver.pay.controller.v1

import com.fasterxml.jackson.databind.ObjectMapper
import com.naver.pay.controller.exception.GlobalExceptionHandler
import com.naver.pay.exception.ExpiredLinkException
import com.naver.pay.service.RedirectService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@ApplyExtension(SpringExtension::class)
@WebMvcTest
class RedirectControllerTest: BehaviorSpec() {
    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var objectMapper: ObjectMapper
    @MockkBean
    lateinit var redirectService: RedirectService

    init {
        Given("리다이렉트 URL 조회를 위한 API에서") {
            When("유효한 shortKey로 리다이렉트를 요청하면") {
                val shortKey = "testKey"
                val originalUrl = "https://naver.com"
                val responseDto = RedirectUrlResponseDto(originalUrl)

                every { redirectService.getRedirectUrl(shortKey) } returns responseDto

                val result = mockMvc.get("/v1/urls/$shortKey")

                Then("200 OK와 함께 원본 URL 정보를 반환한다") {
                    result.andExpect {
                        status { isOk() }
                        content {
                            json(objectMapper.writeValueAsString(responseDto))
                            jsonPath("$.originalUrl") { value(originalUrl) }
                        }
                    }
                }
            }

            When("존재하지 않는 shortKey로 리다이렉트를 요청하면") {
                val shortKey = "nonExistentKey"
                every { redirectService.getRedirectUrl(shortKey) } throws NoSuchElementException("$shortKey 에 해당하는 URL을 찾을 수 없습니다.")

                val result = mockMvc.get("/v1/urls/$shortKey")

                Then("404 Not Found를 반환한다") {
                    result.andExpect {
                        status { isNotFound() }
                    }
                }
            }

            When("만료된 shortKey로 리다이렉트를 요청하면") {
                val shortKey = "expiredKey"
                val originalUrl = "https://naver.com"
                every { redirectService.getRedirectUrl(shortKey) } throws ExpiredLinkException(originalUrl)

                val result = mockMvc.get("/v1/urls/$shortKey")

                Then("410 Gone을 반환한다") {
                    result.andExpect {
                        status { isGone() }
                    }
                }
            }
        }
    }
}
