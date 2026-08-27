package com.armsone.starmanager

import com.armsone.starmanager.service.DirectAIProvider
import com.armsone.starmanager.ui.composer.AIChoice
import com.armsone.starmanager.ui.composer.CaptionSource
import com.armsone.starmanager.ui.composer.captionSource
import com.armsone.starmanager.ui.externalai.ExternalAIAnswerCleaner
import com.armsone.starmanager.ui.externalai.ExternalAIInjectionResult
import com.armsone.starmanager.ui.externalai.ExternalAIPollResult
import com.armsone.starmanager.ui.externalai.ExternalAIScripts
import com.armsone.starmanager.ui.externalai.ExternalAISecurityPolicy
import com.armsone.starmanager.ui.externalai.ExternalAIStabilityReducer
import com.armsone.starmanager.ui.externalai.ExternalAIStabilityState
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalAIParityTest {

    @Test
    fun `노출되는 외부 AI 제공사 목록과 순서가 정확하다`() {
        val visible = DirectAIProvider.visibleProviders
        assertEquals(3, visible.size)
        assertEquals(listOf(DirectAIProvider.GEMINI, DirectAIProvider.OPEN_AI, DirectAIProvider.CLAUDE), visible)
        assertEquals(listOf("Gemini", "ChatGPT", "Claude"), visible.map { it.title })
    }

    @Test
    fun `제공사 공식 URL이 정확히 설정되어 있다`() {
        assertEquals("https://gemini.google.com/app", DirectAIProvider.GEMINI.url)
        assertEquals("https://chatgpt.com/", DirectAIProvider.OPEN_AI.url)
        assertEquals("https://claude.ai/new", DirectAIProvider.CLAUDE.url)
    }

    @Test
    fun `Grok은 사용자 노출 선택지에서 완전히 제외된다`() {
        assertFalse(DirectAIProvider.visibleProviders.contains(DirectAIProvider.GROK))

        val aiChoices = AIChoice.all
        assertFalse(aiChoices.any { it.title == "Grok" || it.id == "grok" })
    }

    @Test
    fun `AIChoice 순서와 기기 AI 표기가 정확하다`() {
        val choices = AIChoice.all
        assertEquals(4, choices.size)
        assertEquals("gemini", choices[0].id)
        assertEquals("Gemini", choices[0].title)

        assertEquals("openAI", choices[1].id)
        assertEquals("ChatGPT", choices[1].title)

        assertEquals("claude", choices[2].id)
        assertEquals("Claude", choices[2].title)

        assertEquals("device-ai", choices[3].id)
        assertEquals("기기 AI", choices[3].title)
    }

    @Test
    fun `CaptionSource 매핑이 제공사별로 올바르다`() {
        assertEquals(CaptionSource.GEMINI, DirectAIProvider.GEMINI.captionSource)
        assertEquals(CaptionSource.CHAT_GPT, DirectAIProvider.OPEN_AI.captionSource)
        assertEquals(CaptionSource.CLAUDE, DirectAIProvider.CLAUDE.captionSource)
        assertEquals(CaptionSource.GROK, DirectAIProvider.GROK.captionSource)
    }

    @Test
    fun `답변 정리기는 마크다운 코드 펜스를 제거한다`() {
        val raw = """
            ```markdown
            #오늘의기록 #따뜻한하루
            별일 없던 하루에도 남기고 싶은 온기가 있다.
            🧡 온기를 담아 오늘을 남긴다 🧡
            ```
        """.trimIndent()

        val expected = """
            #오늘의기록 #따뜻한하루
            별일 없던 하루에도 남기고 싶은 온기가 있다.
            🧡 온기를 담아 오늘을 남긴다 🧡
        """.trimIndent()

        assertEquals(expected, ExternalAIAnswerCleaner.clean(raw))
    }

    @Test
    fun `답변 정리기는 대화형 서두 안내 문구를 제거한다`() {
        val raw = """
            다음은 요청하신 인스타그램 본문입니다:

            #일상기록 #카페나들이
            조용한 공간에서 생각을 정리했다.
            🌿 고요하게 채운 하루의 기록 🌿
        """.trimIndent()

        val expected = """
            #일상기록 #카페나들이
            조용한 공간에서 생각을 정리했다.
            🌿 고요하게 채운 하루의 기록 🌿
        """.trimIndent()

        assertEquals(expected, ExternalAIAnswerCleaner.clean(raw))
    }

    @Test
    fun `답변 정리기는 think 태그와 감싼 따옴표를 제거한다`() {
        val raw = """
            <think>
            User asked for instagram post prose.
            Let's write hashtags first.
            </think>
            "#기록 #일상
            작은 순간이 나를 다독인다.
            😎 오늘도 완벽한 하루 😎"
        """.trimIndent()

        val expected = """
            #기록 #일상
            작은 순간이 나를 다독인다.
            😎 오늘도 완벽한 하루 😎
        """.trimIndent()

        assertEquals(expected, ExternalAIAnswerCleaner.clean(raw))
    }

    @Test
    fun `보안 정책은 허용된 호스트만 통과시키고 비인가 도메인은 차단한다`() {
        assertTrue(ExternalAISecurityPolicy.isAllowedUrl("https://gemini.google.com/app", DirectAIProvider.GEMINI))
        assertTrue(ExternalAISecurityPolicy.isAllowedUrl("https://accounts.google.com/signin", DirectAIProvider.GEMINI))
        assertTrue(ExternalAISecurityPolicy.isAllowedUrl("https://chatgpt.com/", DirectAIProvider.OPEN_AI))
        assertTrue(ExternalAISecurityPolicy.isAllowedUrl("https://auth0.openai.com/u/login", DirectAIProvider.OPEN_AI))
        assertTrue(ExternalAISecurityPolicy.isAllowedUrl("https://claude.ai/new", DirectAIProvider.CLAUDE))
        assertTrue(ExternalAISecurityPolicy.isAllowedUrl("https://appleid.apple.com/auth/authorize", DirectAIProvider.CLAUDE))

        // 차단 케이스 (비인가 도메인, HTTP, 자바스크립트 스킴)
        assertFalse(ExternalAISecurityPolicy.isAllowedUrl("http://gemini.google.com/app", DirectAIProvider.GEMINI))
        assertFalse(ExternalAISecurityPolicy.isAllowedUrl("https://malicious-site.com/login", DirectAIProvider.GEMINI))
        assertFalse(ExternalAISecurityPolicy.isAllowedUrl("javascript:alert(1)", DirectAIProvider.OPEN_AI))
        assertFalse(ExternalAISecurityPolicy.isAllowedUrl(null, DirectAIProvider.CLAUDE))
    }

    @Test
    fun `프롬프트 주입 스크립트에 기준선 기록 및 구조화된 JSON 반환이 포함되어 있다`() {
        val script = ExternalAIScripts.injectPromptScript(DirectAIProvider.GEMINI, "테스트 요청문")
        assertTrue(script.contains("window.__sm_ai_baseline"))
        assertTrue(script.contains("JSON.stringify"))
        assertTrue(script.contains("inputFound"))
        assertTrue(script.contains("submitted"))
        assertTrue(script.contains("INPUT_NOT_FOUND"))
        assertTrue(script.contains("#prompt-textarea"))
        assertTrue(script.contains("button[data-testid=\"send-button\"]"))
    }

    @Test
    fun `답변 추출 스크립트에 기준선 검증 및 생성 인디케이터 탐색이 포함되어 있다`() {
        val script = ExternalAIScripts.extractAnswerScript(DirectAIProvider.OPEN_AI)
        assertTrue(script.contains("window.__sm_ai_baseline"))
        assertTrue(script.contains("newAnswer"))
        assertTrue(script.contains("generating"))
        assertTrue(script.contains("button[data-testid=\"stop-button\"]"))
        assertTrue(script.contains("div[data-is-streaming=\"true\"]"))
        assertTrue(script.contains("JSON.stringify"))
    }

    @Test
    fun `독립 기준선 기록 스크립트가 유효하게 정의되어 있다`() {
        val script = ExternalAIScripts.recordBaselineScript()
        assertTrue(script.contains("window.__sm_ai_baseline"))
        assertTrue(script.contains("JSON.stringify"))
        assertTrue(script.contains("div[data-message-author-role=\"assistant\"]"))
    }

    @Test
    fun `JSON 파싱 헬퍼는 evaluateJavascript 반환 형식의 주입 결과를 안전하게 디코딩한다`() {
        // 1. 정상 전송 성공 (evaluateJavascript가 직렬화한 JSON 문자열)
        val rawSuccess = "\"{\\\"success\\\":true,\\\"inputFound\\\":true,\\\"submitted\\\":true}\""
        val successRes = ExternalAIScripts.parseInjectionResult(rawSuccess)
        assertTrue(successRes.success)
        assertTrue(successRes.inputFound)
        assertTrue(successRes.submitted)
        assertNull(successRes.error)

        // 2. 자동 전송 실패 (입력은 성공했으나 전송 버튼 미클릭)
        val rawUnsubmitted = "\"{\\\"success\\\":true,\\\"inputFound\\\":true,\\\"submitted\\\":false}\""
        val unsubmittedRes = ExternalAIScripts.parseInjectionResult(rawUnsubmitted)
        assertTrue(unsubmittedRes.success)
        assertTrue(unsubmittedRes.inputFound)
        assertFalse(unsubmittedRes.submitted)

        // 3. 입력창 미발견
        val rawNotFound = "\"{\\\"success\\\":false,\\\"inputFound\\\":false,\\\"submitted\\\":false,\\\"error\\\":\\\"INPUT_NOT_FOUND\\\"}\""
        val notFoundRes = ExternalAIScripts.parseInjectionResult(rawNotFound)
        assertFalse(notFoundRes.success)
        assertFalse(notFoundRes.inputFound)
        assertFalse(notFoundRes.submitted)
        assertEquals("INPUT_NOT_FOUND", notFoundRes.error)

        // 4. 레거시 문자열 호환성
        val legacySuccess = "\"INPUT_SUCCESS\""
        val legacySuccessRes = ExternalAIScripts.parseInjectionResult(legacySuccess)
        assertTrue(legacySuccessRes.success)
        assertTrue(legacySuccessRes.submitted)

        val legacyNotFound = "\"INPUT_NOT_FOUND\""
        val legacyNotFoundRes = ExternalAIScripts.parseInjectionResult(legacyNotFound)
        assertFalse(legacyNotFoundRes.inputFound)

        // 5. null 또는 깨진 문자열
        val nullRes = ExternalAIScripts.parseInjectionResult(null)
        assertFalse(nullRes.success)
        val malformedRes = ExternalAIScripts.parseInjectionResult("{invalid-json")
        assertFalse(malformedRes.success)
    }

    @Test
    fun `JSON 파싱 헬퍼는 evaluateJavascript 반환 형식의 폴링 결과를 안전하게 디코딩한다`() {
        // 1. 특수문자, 줄바꿈, 따옴표가 포함된 새 답변
        val gson = Gson()
        val innerJson = JsonObject().apply {
            addProperty("newAnswer", true)
            addProperty("generating", false)
            addProperty("text", "#일상 #기록\n따뜻한 하루\n\"인용구\"")
        }.toString()
        val rawValid = gson.toJson(innerJson)
        val validRes = ExternalAIScripts.parsePollResult(rawValid)
        assertTrue(validRes.newAnswer)
        assertFalse(validRes.generating)
        assertEquals("#일상 #기록\n따뜻한 하루\n\"인용구\"", validRes.text)

        // 2. 생성 중(스트리밍) 상태
        val rawGenerating = "\"{\\\"newAnswer\\\":true,\\\"generating\\\":true,\\\"text\\\":\\\"답변 작성 중...\\\"}\""
        val genRes = ExternalAIScripts.parsePollResult(rawGenerating)
        assertTrue(genRes.newAnswer)
        assertTrue(genRes.generating)
        assertEquals("답변 작성 중...", genRes.text)

        // 3. 새 답변 없음
        val rawNoAnswer = "\"{\\\"newAnswer\\\":false,\\\"generating\\\":false,\\\"text\\\":\\\"\\\"}\""
        val noAnswerRes = ExternalAIScripts.parsePollResult(rawNoAnswer)
        assertFalse(noAnswerRes.newAnswer)
        assertFalse(noAnswerRes.generating)
        assertEquals("", noAnswerRes.text)

        // 4. 예외 케이스 방어
        val nullRes = ExternalAIScripts.parsePollResult(null)
        assertFalse(nullRes.newAnswer)
        assertFalse(nullRes.generating)
        assertEquals("", nullRes.text)
    }

    @Test
    fun `안정성 리듀서는 새 답변이 없거나 생성 중일 때 카운터를 즉시 리셋한다`() {
        val initial = ExternalAIStabilityState()

        // 새 답변 없음
        val noAnswerPoll = ExternalAIPollResult(newAnswer = false, generating = false, text = "")
        val state1 = ExternalAIStabilityReducer.step(initial, noAnswerPoll)
        assertEquals(0, state1.consecutiveMatches)
        assertFalse(state1.isStable)
        assertNull(state1.stableAnswer)

        // 생성 중 상태 (텍스트가 존재해도 카운터는 0)
        val generatingPoll = ExternalAIPollResult(
            newAnswer = true,
            generating = true,
            text = "#해시태그 #긴텍스트본문이작성되는중입니다12345"
        )
        val state2 = ExternalAIStabilityReducer.step(initial, generatingPoll)
        assertEquals(0, state2.consecutiveMatches)
        assertFalse(state2.isStable)
        assertNull(state2.stableAnswer)
    }

    @Test
    fun `안정성 리듀서는 최소 길이 미만인 답변에 대해 안정 상태로 진입하지 않는다`() {
        val shortPoll = ExternalAIPollResult(
            newAnswer = true,
            generating = false,
            text = "짧은답변"
        )
        var state = ExternalAIStabilityState()
        for (i in 1..5) {
            state = ExternalAIStabilityReducer.step(state, shortPoll)
            assertFalse(state.isStable)
            assertNull(state.stableAnswer)
            assertEquals(0, state.consecutiveMatches)
        }
    }

    @Test
    fun `안정성 리듀서는 3회 연속 동일 답변이 관측될 때만 COMPLETED 안정 상태로 전환된다`() {
        val sampleText = """
            #스타매니저 #완벽한기록
            오늘 하루도 최선을 다해 멋지게 마무리했습니다.
        """.trimIndent()
        val poll = ExternalAIPollResult(
            newAnswer = true,
            generating = false,
            text = sampleText
        )

        var state = ExternalAIStabilityState()

        // 1회 관측
        state = ExternalAIStabilityReducer.step(state, poll)
        assertEquals(1, state.consecutiveMatches)
        assertFalse(state.isStable)
        assertNull(state.stableAnswer)

        // 2회 관측
        state = ExternalAIStabilityReducer.step(state, poll)
        assertEquals(2, state.consecutiveMatches)
        assertFalse(state.isStable)
        assertNull(state.stableAnswer)

        // 3회 관측 -> 안정 통과!
        state = ExternalAIStabilityReducer.step(state, poll)
        assertEquals(3, state.consecutiveMatches)
        assertTrue(state.isStable)
        assertEquals(sampleText, state.stableAnswer)

        // 4회 관측 시 텍스트 변경 -> 안정성 즉시 해제 및 카운터 재설정
        val changedPoll = ExternalAIPollResult(
            newAnswer = true,
            generating = false,
            text = sampleText + "\n추가 수정된 문장입니다."
        )
        state = ExternalAIStabilityReducer.step(state, changedPoll)
        assertEquals(1, state.consecutiveMatches)
        assertFalse(state.isStable)
        assertNull(state.stableAnswer)

        // 5회 관측 시 생성 중 재개 -> 카운터 0으로 리셋
        val genPoll = ExternalAIPollResult(
            newAnswer = true,
            generating = true,
            text = sampleText + "\n추가 수정된 문장입니다."
        )
        state = ExternalAIStabilityReducer.step(state, genPoll)
        assertEquals(0, state.consecutiveMatches)
        assertFalse(state.isStable)
        assertNull(state.stableAnswer)
    }

    @Test
    fun `안정성 상태 reset은 모든 필드를 초기화한다`() {
        val activeState = ExternalAIStabilityState(
            consecutiveMatches = 3,
            lastCleanedText = "기존 답변",
            isStable = true,
            stableAnswer = "기존 답변"
        )
        val resetState = activeState.reset()
        assertEquals(0, resetState.consecutiveMatches)
        assertEquals("", resetState.lastCleanedText)
        assertFalse(resetState.isStable)
        assertNull(resetState.stableAnswer)
    }
}
