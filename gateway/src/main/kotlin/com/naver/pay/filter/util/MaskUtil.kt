package com.naver.pay.filter.util

/**
 * 민감한 정보를 마스킹하는 유틸리티 클래스
 */
object MaskUtil {

    /**
     * 문자열의 앞부분과 뒷부분 일부만 보여주고 나머지를 마스킹합니다.
     *
     * @param value 마스킹할 문자열
     * @param prefixLength 앞부분에 보여줄 문자 수 (기본값: 4)
     * @param suffixLength 뒷부분에 보여줄 문자 수 (기본값: 4)
     * @param maskChar 마스킹에 사용할 문자 (기본값: '*')
     * @return 마스킹된 문자열
     *
     * 예시:
     * - mask("abcdefghijklmnop", 4, 4) -> "abcd...mnop"
     * - mask("short", 4, 4) -> "***"
     * - mask("abcdefghijklmnop", 2, 2) -> "ab...op"
     */
    fun mask(
        value: String,
        prefixLength: Int = 4,
        suffixLength: Int = 4,
        maskChar: Char = '*'
    ): String {
        if (value.length <= prefixLength + suffixLength) {
            return maskChar.toString().repeat(3)
        }

        val prefix = value.take(prefixLength)
        val suffix = value.takeLast(suffixLength)
        return "$prefix...$suffix"
    }

    /**
     * API Key를 마스킹합니다.
     *
     * @param apiKey 마스킹할 API Key
     * @return 마스킹된 API Key
     */
    fun maskApiKey(apiKey: String): String {
        return mask(apiKey, prefixLength = 4, suffixLength = 4)
    }
}