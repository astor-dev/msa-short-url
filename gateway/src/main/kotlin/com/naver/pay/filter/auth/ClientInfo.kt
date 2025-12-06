package com.naver.pay.filter.auth

data class ClientInfo(
    val clientId: String,
    val role: ClientRole,
) {
    companion object {
        fun createAnonymous(): ClientInfo {
            return ClientInfo(
                clientId = "anonymous",
                role = ClientRole.ANONYMOUS
            )
        }
    }
}

