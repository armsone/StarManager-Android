package com.armsone.starmanager

import com.armsone.starmanager.ui.externalai.ExternalAIErrorSanitizer
import com.armsone.starmanager.ui.externalai.ExternalAIFallbackClassifier
import com.armsone.starmanager.ui.externalai.ExternalAIFallbackReason
import com.armsone.starmanager.ui.externalai.ExternalAITimerFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gemini 초안 타임아웃 이후 추가된 순수 유틸리티(에러 정제기, 타이머 포맷터, 폴백 분류기)에 대한
 * 결정적 JVM 단위 테스트. WebView/네트워크 없이 순수 함수만 검증한다.
 */
class ExternalAIDraftUtilsTest {

    // MARK: - ExternalAIErrorSanitizer

    @Test
    fun `오류 정제기는 HTML 태그와 script 스타일 블록을 제거한다`() {
        val raw = "<div>오류 발생<script>alert(1)</script><style>.a{}</style></div>"
        val result = ExternalAIErrorSanitizer.sanitize(raw)
        assertFalse(result.contains("<"))
        assertFalse(result.contains("alert"))
        assertTrue(result.contains("오류 발생"))
    }

    @Test
    fun `오류 정제기는 URL과 토큰 비밀 패턴을 제거한다`() {
        val raw = "요청 실패: https://example.com/api?token=abcd1234efgh5678 bearer eyJhbGciOiJIUzI1NiJ9"
        val result = ExternalAIErrorSanitizer.sanitize(raw)
        assertFalse(result.contains("https://"))
        assertFalse(result.contains("bearer", ignoreCase = true))
        assertFalse(result.contains("token="))
    }

    @Test
    fun `오류 정제기는 스택 트레이스 줄을 제거한다`() {
        val raw = """
            요청 처리 중 오류가 발생했습니다
            at com.example.Foo.bar(Foo.java:42)
            java.lang.NullPointerException: null
        """.trimIndent()
        val result = ExternalAIErrorSanitizer.sanitize(raw)
        assertFalse(result.contains("at com.example"))
        assertFalse(result.contains("NullPointerException"))
        assertTrue(result.contains("요청 처리 중 오류가 발생했습니다"))
    }

    @Test
    fun `오류 정제기는 공백만 있거나 정제 후 빈 문자열이면 기본 메시지를 반환한다`() {
        assertEquals("오류가 발생했어요. 다시 시도해 주세요.", ExternalAIErrorSanitizer.sanitize(null))
        assertEquals("오류가 발생했어요. 다시 시도해 주세요.", ExternalAIErrorSanitizer.sanitize(""))
        assertEquals("오류가 발생했어요. 다시 시도해 주세요.", ExternalAIErrorSanitizer.sanitize("   "))
        assertEquals("오류가 발생했어요. 다시 시도해 주세요.", ExternalAIErrorSanitizer.sanitize("<script>alert(1)</script>"))
    }

    @Test
    fun `오류 정제기는 최대 길이를 초과하면 말줄임표로 축약한다`() {
        val longText = "가".repeat(200)
        val result = ExternalAIErrorSanitizer.sanitize(longText, maxLength = 80)
        assertEquals(80, result.length)
        assertTrue(result.endsWith("…"))
    }

    @Test
    fun `오류 정제기는 흔한 접두사를 제거한다`() {
        assertEquals("서버가 응답하지 않습니다", ExternalAIErrorSanitizer.sanitize("Error: 서버가 응답하지 않습니다"))
        assertEquals("서버가 응답하지 않습니다", ExternalAIErrorSanitizer.sanitize("오류: 서버가 응답하지 않습니다"))
    }

    // MARK: - ExternalAITimerFormatter

    @Test
    fun `타이머 포맷터는 60초 미만을 두 자리 초로 표시한다`() {
        assertEquals("00초", ExternalAITimerFormatter.formatElapsedSeconds(0))
        assertEquals("05초", ExternalAITimerFormatter.formatElapsedSeconds(5))
        assertEquals("59초", ExternalAITimerFormatter.formatElapsedSeconds(59))
    }

    @Test
    fun `타이머 포맷터는 60초 이상을 분과 초로 표시한다`() {
        assertEquals("1분 00초", ExternalAITimerFormatter.formatElapsedSeconds(60))
        assertEquals("1분 05초", ExternalAITimerFormatter.formatElapsedSeconds(65))
        assertEquals("2분 30초", ExternalAITimerFormatter.formatElapsedSeconds(150))
    }

    @Test
    fun `타이머 포맷터는 음수를 0초로 clamp한다`() {
        assertEquals("00초", ExternalAITimerFormatter.formatElapsedSeconds(-5))
    }

    @Test
    fun `대기 상태 문구는 1분 59초 역카운터를 반환한다`() {
        assertEquals("남은 시간 1:59", ExternalAITimerFormatter.formatWaitingStatus(0))
        assertEquals("남은 시간 0:59", ExternalAITimerFormatter.formatWaitingStatus(60))
        assertEquals("남은 시간 0:00", ExternalAITimerFormatter.formatWaitingStatus(119))
    }

    // MARK: - ExternalAIFallbackClassifier

    @Test
    fun `URL 분류기는 로그인 관련 호스트를 LOGIN_REQUIRED로 분류한다`() {
        assertEquals(
            ExternalAIFallbackReason.LOGIN_REQUIRED,
            ExternalAIFallbackClassifier.classifyUrl("https://accounts.google.com/signin")
        )
        assertEquals(
            ExternalAIFallbackReason.LOGIN_REQUIRED,
            ExternalAIFallbackClassifier.classifyUrl("https://auth0.openai.com/u/login")
        )
        assertEquals(
            ExternalAIFallbackReason.LOGIN_REQUIRED,
            ExternalAIFallbackClassifier.classifyUrl("https://appleid.apple.com/auth/authorize")
        )
    }

    @Test
    fun `URL 분류기는 보안 챌린지 호스트를 SECURITY_VERIFICATION으로 분류한다`() {
        assertEquals(
            ExternalAIFallbackReason.SECURITY_VERIFICATION,
            ExternalAIFallbackClassifier.classifyUrl("https://challenges.cloudflare.com/turnstile")
        )
        assertEquals(
            ExternalAIFallbackReason.SECURITY_VERIFICATION,
            ExternalAIFallbackClassifier.classifyUrl("https://www.google.com/recaptcha/api2/anchor")
        )
    }

    @Test
    fun `URL 분류기는 일반 제공사 URL과 null 빈 문자열에 대해 null을 반환한다`() {
        assertNull(ExternalAIFallbackClassifier.classifyUrl("https://gemini.google.com/app"))
        assertNull(ExternalAIFallbackClassifier.classifyUrl("https://chatgpt.com/"))
        assertNull(ExternalAIFallbackClassifier.classifyUrl(null))
        assertNull(ExternalAIFallbackClassifier.classifyUrl(""))
    }

    @Test
    fun `DOM 상태 분류기는 우선순위에 따라 로그인, 보안, 입력, 확인 사유를 판별한다`() {
        assertEquals(
            ExternalAIFallbackReason.LOGIN_REQUIRED,
            ExternalAIFallbackClassifier.classifyDomState(
                isAuthRequired = true,
                isSecurityChallenge = true,
                inputFound = false,
                submitted = false
            )
        )
        assertEquals(
            ExternalAIFallbackReason.SECURITY_VERIFICATION,
            ExternalAIFallbackClassifier.classifyDomState(
                isAuthRequired = false,
                isSecurityChallenge = true,
                inputFound = false,
                submitted = false
            )
        )
        assertEquals(
            ExternalAIFallbackReason.MANUAL_INPUT_REQUIRED,
            ExternalAIFallbackClassifier.classifyDomState(
                isAuthRequired = false,
                isSecurityChallenge = false,
                inputFound = false,
                submitted = false
            )
        )
        assertEquals(
            ExternalAIFallbackReason.MANUAL_CONFIRMATION,
            ExternalAIFallbackClassifier.classifyDomState(
                isAuthRequired = false,
                isSecurityChallenge = false,
                inputFound = true,
                submitted = false
            )
        )
        assertNull(
            ExternalAIFallbackClassifier.classifyDomState(
                isAuthRequired = false,
                isSecurityChallenge = false,
                inputFound = true,
                submitted = true
            )
        )
    }
}
