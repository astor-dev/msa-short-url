package com.naver.pay.auth

enum class ClientRole {
    ADMIN,     // 모든 API 접근 가능
    USER,      // 통계 API 제외 접근 가능
    ANONYMOUS  // 리다이렉트 API만 호출 가능
}