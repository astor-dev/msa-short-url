package com.naver.pay.filter.auth

/**
 * 경로별 접근 권한 정책을 관리하는 클래스
 * 
 * 각 API 경로에 대해 필요한 최소 권한(Role)을 정의합니다.
 */
class PathAuthorizationPolicy {
    
    /**
     * 경로 패턴과 필요한 최소 권한을 매핑합니다.
     * 
     * 주의사항:
     * - firstOrNull()을 사용하여 위에서부터 순차적으로 매칭을 시도합니다.
     * - 더 구체적인 패턴을 위에 배치해야 합니다
     * - 매칭되는 첫 번째 패턴의 권한이 적용됩니다.
     */
    private val pathRoleMap: Map<Regex, ClientRole> = mapOf(
        // 통계 API: ADMIN만 접근 가능
        // 더 구체적인 패턴을 먼저 배치
        Regex("^/api/v1/urls/.*/statistics$") to ClientRole.ADMIN,
        Regex("^/api/v1/statistics/.*") to ClientRole.ADMIN,
        
        // 리다이렉트 API: 모든 사용자 접근 가능 (ANONYMOUS 포함)
        // 단일 경로 패턴 (shortKey만 있는 경로)
        Regex("^/[^/]+$") to ClientRole.ANONYMOUS,
        
        // URL 생성/조회 API: USER 이상 접근 가능
        // 더 구체적인 패턴을 먼저 배치
        Regex("^/api/v1/urls$") to ClientRole.USER,
        Regex("^/api/v1/urls/.*") to ClientRole.USER,
    )
    
    /**
     * Actuator 엔드포인트는 인가 제외
     */
    private val excludedPaths = listOf("/actuator")
    
    /**
     * 클라이언트가 해당 경로에 접근할 수 있는지 확인합니다.
     * 
     * @param clientInfo 클라이언트 정보
     * @param path 요청 경로
     * @return 접근 가능 여부
     */
    fun canAccess(clientInfo: ClientInfo, path: String): Boolean {
        // Actuator 엔드포인트는 모든 사용자 접근 가능
        if (excludedPaths.any { path.startsWith(it) }) {
            return true
        }
        
        // 경로에 매칭되는 권한 요구사항 찾기
        val requiredRole = findRequiredRole(path)
        
        // 권한 요구사항이 없으면 기본적으로 USER 이상 필요
        val minimumRole = requiredRole ?: ClientRole.USER
        
        // 클라이언트의 역할이 최소 권한 이상인지 확인
        return hasMinimumRole(clientInfo.role, minimumRole)
    }
    
    /**
     * 경로에 필요한 최소 권한을 찾습니다. 매칭되는 패턴이 없으면 null을 반환합니다.
     */
    private fun findRequiredRole(path: String): ClientRole? {
        return pathRoleMap.entries
            .firstOrNull { it.key.matches(path) }
            ?.value
    }
    
    /**
     * 클라이언트 역할이 최소 권한 이상인지 확인합니다.
     */
    private fun hasMinimumRole(clientRole: ClientRole, minimumRole: ClientRole): Boolean {
        val roleHierarchy = listOf(ClientRole.ANONYMOUS, ClientRole.USER, ClientRole.ADMIN)
        val clientRoleIndex = roleHierarchy.indexOf(clientRole)
        val minimumRoleIndex = roleHierarchy.indexOf(minimumRole)
        
        return clientRoleIndex >= minimumRoleIndex
    }
    
    /**
     * 접근이 거부된 경우 반환할 에러 메시지를 생성합니다.
     */
    fun getAccessDeniedMessage(clientInfo: ClientInfo, path: String): String {
        val requiredRole = findRequiredRole(path) ?: ClientRole.USER
        
        return when (requiredRole) {
            ClientRole.ADMIN if clientInfo.role != ClientRole.ADMIN ->
                "접근 거부: 통계 API는 ADMIN 권한이 필요합니다"

            ClientRole.USER if clientInfo.role == ClientRole.ANONYMOUS ->
                "접근 거부: 이 API는 인증이 필요합니다"

            else -> "접근 거부: 권한이 부족합니다. 필요한 권한: ${requiredRole.name}"
        }
    }
}
