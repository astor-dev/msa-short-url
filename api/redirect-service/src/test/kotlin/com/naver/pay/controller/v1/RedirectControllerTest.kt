package com.naver.pay.controller.v1

import com.naver.pay.exception.ExpiredLinkException
import com.naver.pay.service.RedirectService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.NoSuchElementException

@ApplyExtension(SpringExtension::class)
@WebMvcTest(RedirectController::class)
class RedirectControllerTest : BehaviorSpec() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var redirectService: RedirectService

    init {
        Given("리다이렉트 URL 조회를 위한 API에서") {
            When("유효한 shortKey로 리다이렉트를 요청하면") {
                val shortKey = "testKey"
                val originalUrl = "https://naver.com"

                every { redirectService.getRedirectUrl(shortKey, null, null) } returns originalUrl

                val result = mockMvc.get("/v1/urls/$shortKey")

                Then("302 Found와 함께 Location 헤더에 원본 URL을 담아 반환한다") {
                    result.andExpect {
                        status { isFound() }
                        header {
                            string("Location", originalUrl)
                        }
                    }
                }
            }

            When("유효한 shortKey와 헤더로 리다이렉트를 요청하면") {
                val shortKey = "testKeyWithHeaders"
                val originalUrl = "https://naver.com"
                val userAgent = "Test-Agent"
                val referrer = "https://test.com"

                every { redirectService.getRedirectUrl(shortKey, userAgent, referrer) } returns originalUrl

                val result = mockMvc.get("/v1/urls/$shortKey") {
                    header("User-Agent", userAgent)
                    header("Referer", referrer)
                }

                Then("302 Found와 함께 Location 헤더에 원본 URL을 담아 반환한다") {
                    result.andExpect {
                        status { isFound() }
                        header {
                            string("Location", originalUrl)
                        }
                    }
                }
            }

            When("존재하지 않는 shortKey로 리다이렉트를 요청하면") {
                val shortKey = "nonExistentKey"
                every {
                    redirectService.getRedirectUrl(
                        shortKey,
                        null,
                        null
                    )
                } throws NoSuchElementException("$shortKey 에 해당하는 URL을 찾을 수 없습니다.")

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
                every { redirectService.getRedirectUrl(shortKey, null, null) } throws ExpiredLinkException(originalUrl)

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