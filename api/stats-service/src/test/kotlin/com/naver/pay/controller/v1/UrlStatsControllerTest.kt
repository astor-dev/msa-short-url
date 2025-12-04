package com.naver.pay.controller.v1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.naver.pay.service.ShortUrlStatsService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.NoSuchElementException

@ApplyExtension(SpringExtension::class)
@WebMvcTest(UrlStatsController::class)
class UrlStatsControllerTest : BehaviorSpec() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var shortUrlStatsService: ShortUrlStatsService

    // Register JavaTimeModule to handle Instant serialization
    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    init {
        Given("단축 URL 통계 조회를 위한 API에서") {
            val shortKey = "testKey"

            When("유효한 shortKey로 상태 조회를 요청하면") {
                val responseDto = ShortUrlStateResponseDto(
                    shortKey = shortKey,
                    originalUrl = "https://naver.com",
                    shortUrl = "http://localhost/testKey",
                    createdAt = "2025-12-04T10:00:00Z",
                    expiresAt = "2025-12-05T10:00:00Z",
                    clickSummary = ClickSummaryResponseDto(
                        totalClicks = 150L,
                        lastClickedAt = "2025-12-04T11:00:00Z"
                    )
                )

                every { shortUrlStatsService.findShortUrlStateOrThrow(shortKey) } returns responseDto

                val result = mockMvc.get("/v1/urls/$shortKey")

                Then("200 OK와 함께 상태 정보를 반환한다") {
                    result.andExpect {
                        status { isOk() }
                        content {
                            contentType(MediaType.APPLICATION_JSON)
                            json(objectMapper.writeValueAsString(responseDto))
                        }
                    }
                }
            }

            When("존재하지 않는 shortKey로 상태 조회를 요청하면") {
                every { shortUrlStatsService.findShortUrlStateOrThrow(shortKey) } throws NoSuchElementException("$shortKey 에 해당하는 URL을 찾을 수 없습니다.")

                val result = mockMvc.get("/v1/urls/$shortKey")

                Then("404 Not Found를 반환한다") {
                    result.andExpect {
                        status { isNotFound() }
                    }
                }
            }

            When("유효한 shortKey로 통계 조회를 요청하면") {
                val responseDto = ShortUrlStatisticsResponseDto(
                    shortKey = shortKey,
                    totalClicks = 1234L,
                    byDate = listOf(
                        ShortUrlStatisticsByDateResponseDto(
                            date = "2025-12-04",
                            clicks = 100
                        )
                    ),
                    byReferrer = listOf(
                        ShortUrlStatisticsByReferrerResponseDto(
                            referrer = "https://google.com",
                            clicks = 50
                        )
                    ),
                    byDevice = listOf(
                        ShortUrlStatisticsByDeviceTypeResponseDto(
                            deviceType = "PC",
                            clicks = 70
                        )
                    )
                )

                every { shortUrlStatsService.findShortUrlTotalStatsOrThrow(shortKey) } returns responseDto

                val result = mockMvc.get("/v1/urls/$shortKey/statistics")

                Then("200 OK와 함께 통계 정보를 반환한다") {
                    result.andExpect {
                        status { isOk() }
                        content {
                            contentType(MediaType.APPLICATION_JSON)
                            json(objectMapper.writeValueAsString(responseDto))
                        }
                    }
                }
            }

            When("존재하지 않는 shortKey로 통계 조회를 요청하면") {
                every { shortUrlStatsService.findShortUrlTotalStatsOrThrow(shortKey) } throws NoSuchElementException("Short URL not found: $shortKey")

                val result = mockMvc.get("/v1/urls/$shortKey/statistics")

                Then("404 Not Found를 반환한다") {
                    result.andExpect {
                        status { isNotFound() }
                    }
                }
            }
        }
    }
}
