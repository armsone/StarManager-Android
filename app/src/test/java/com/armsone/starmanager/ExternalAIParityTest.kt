package com.armsone.starmanager

import com.armsone.starmanager.service.DirectAIProvider
import com.armsone.starmanager.ui.composer.AIChoice
import com.armsone.starmanager.ui.composer.CaptionSource
import com.armsone.starmanager.ui.composer.captionSource
import com.armsone.starmanager.ui.externalai.ExternalAIAnswerCleaner
import com.armsone.starmanager.ui.externalai.ExternalAIFallbackReason
import com.armsone.starmanager.ui.externalai.ExternalAIInjectionResult
import com.armsone.starmanager.ui.externalai.ExternalAIPollResult
import com.armsone.starmanager.ui.externalai.ExternalAIScripts
import com.armsone.starmanager.ui.externalai.ExternalAISecurityPolicy
import com.armsone.starmanager.ui.externalai.ExternalAIStabilityReducer
import com.armsone.starmanager.ui.externalai.ExternalAIStabilityState
import com.armsone.starmanager.ui.externalai.ExternalAITimingProfile
import com.armsone.starmanager.ui.externalai.ExternalAITimerFormatter
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
    fun `AIChoice 순서와 AI 표기가 정확하다`() {
        val choices = AIChoice.all
        assertEquals(4, choices.size)
        assertEquals("gemini", choices[0].id)
        assertEquals("Gemini", choices[0].title)

        assertEquals("openAI", choices[1].id)
        assertEquals("ChatGPT", choices[1].title)

        assertEquals("claude", choices[2].id)
        assertEquals("Claude", choices[2].title)

        assertEquals("device-ai", choices[3].id)
        assertEquals("AI", choices[3].title)
    }

    @Test
    fun `CaptionSource 매핑이 제공사별로 올바르다`() {
        assertEquals(CaptionSource.GEMINI, DirectAIProvider.GEMINI.captionSource)
        assertEquals(CaptionSource.CHAT_GPT, DirectAIProvider.OPEN_AI.captionSource)
        assertEquals(CaptionSource.CLAUDE, DirectAIProvider.CLAUDE.captionSource)
        assertEquals(CaptionSource.GROK, DirectAIProvider.GROK.captionSource)
    }

    @Test
    fun `AIBI 타이밍 프로필 기본값이 정규 규격과 일치한다`() {
        val timings = ExternalAITimingProfile.DEFAULT
        assertEquals(35_000L, timings.readinessTimeoutMs)
        assertEquals(700L, timings.readinessCadenceMs)
        assertEquals(12, timings.maxReadinessMisses)
        assertEquals(15_000L, timings.submitTimeoutMs)
        assertEquals(500L, timings.submitCadenceMs)
        assertEquals(700L, timings.submitVerificationDelayMs)
        assertEquals(45_000L, timings.visibleAutoFillTimeoutMs)
        assertEquals(700L, timings.observationCadenceMs)
        assertEquals(2, timings.stabilityRequiredTicks)
        assertEquals(119L, ExternalAITimerFormatter.GENERATION_TIMEOUT_SECONDS)
        assertEquals("남은 시간 1:59", ExternalAITimerFormatter.formatWaitingStatus(0L))
        assertEquals("남은 시간 0:00", ExternalAITimerFormatter.formatWaitingStatus(119L))
        assertEquals(1f, ExternalAITimerFormatter.progress(0L))
        assertEquals(0f, ExternalAITimerFormatter.progress(119L))
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
    fun `답변 정리기는 대화형 서두 안내 문구와 헤더를 제거한다`() {
        val raw1 = """
            본문: #일상기록 #카페나들이
            조용한 공간에서 생각을 정리했다.
            🌿 고요하게 채운 하루의 기록 🌿
        """.trimIndent()

        val expected1 = """
            #일상기록 #카페나들이
            조용한 공간에서 생각을 정리했다.
            🌿 고요하게 채운 하루의 기록 🌿
        """.trimIndent()

        assertEquals(expected1, ExternalAIAnswerCleaner.clean(raw1))

        val raw2 = """
            답변:
            #일상 #기록
            하루를 마무리하며.
        """.trimIndent()

        val expected2 = """
            #일상 #기록
            하루를 마무리하며.
        """.trimIndent()

        assertEquals(expected2, ExternalAIAnswerCleaner.clean(raw2))
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
    fun `답변 정리기는 Grok 생각 시간 접두사를 제거한다`() {
        val raw = "12s 작업함\n#일상 #기록\n오늘도 보람찬 하루!"
        val expected = "#일상 #기록\n오늘도 보람찬 하루!"
        assertEquals(expected, ExternalAIAnswerCleaner.clean(raw, DirectAIProvider.GROK))
    }

    @Test
    fun `보안 정책은 스크립트 오리진과 인증 오리진을 엄격히 구분한다`() {
        // 스크립트 오리진
        assertTrue(ExternalAISecurityPolicy.isScriptOrigin("https://gemini.google.com/app", DirectAIProvider.GEMINI))
        assertTrue(ExternalAISecurityPolicy.isScriptOrigin("https://chatgpt.com/", DirectAIProvider.OPEN_AI))
        assertTrue(ExternalAISecurityPolicy.isScriptOrigin("https://chat.openai.com/", DirectAIProvider.OPEN_AI))
        assertTrue(ExternalAISecurityPolicy.isScriptOrigin("https://claude.ai/new", DirectAIProvider.CLAUDE))

        // 인증 오리진 (스크립트 주입 불가)
        assertTrue(ExternalAISecurityPolicy.isAuthOrigin("https://accounts.google.com/signin", DirectAIProvider.GEMINI))
        assertTrue(ExternalAISecurityPolicy.isAuthOrigin("https://auth0.openai.com/u/login", DirectAIProvider.OPEN_AI))
        assertTrue(ExternalAISecurityPolicy.isAuthOrigin("https://appleid.apple.com/auth/authorize", DirectAIProvider.CLAUDE))
        assertTrue(ExternalAISecurityPolicy.isAuthOrigin("https://claude.ai/login", DirectAIProvider.CLAUDE))

        // canInjectScript 검증 (인증 오리진에는 절대 주입 금지)
        assertTrue(ExternalAISecurityPolicy.canInjectScript("https://gemini.google.com/app", DirectAIProvider.GEMINI))
        assertFalse(ExternalAISecurityPolicy.canInjectScript("https://accounts.google.com/signin", DirectAIProvider.GEMINI))
        assertFalse(ExternalAISecurityPolicy.canInjectScript("https://auth0.openai.com/u/login", DirectAIProvider.OPEN_AI))
        assertFalse(ExternalAISecurityPolicy.canInjectScript("https://claude.ai/login", DirectAIProvider.CLAUDE))

        // 차단 케이스 (비인가 도메인, HTTP, 자바스크립트 스킴)
        assertFalse(ExternalAISecurityPolicy.isAllowedUrl("http://gemini.google.com/app", DirectAIProvider.GEMINI))
        assertFalse(ExternalAISecurityPolicy.isAllowedUrl("https://malicious-site.com/login", DirectAIProvider.GEMINI))
        assertFalse(ExternalAISecurityPolicy.isAllowedUrl("javascript:alert(1)", DirectAIProvider.OPEN_AI))
        assertFalse(ExternalAISecurityPolicy.isAllowedUrl(null, DirectAIProvider.CLAUDE))
    }

    @Test
    fun `프롬프트 주입 스크립트에 ContentEditable 및 네이티브 세터 지원이 포함되어 있다`() {
        val script = ExternalAIScripts.injectPromptScript(DirectAIProvider.GEMINI, "테스트 요청문", force = false)
        assertTrue(script.contains("HTMLTextAreaElement.prototype"))
        assertTrue(script.contains("isContentEditable"))
        assertTrue(script.contains("JSON.stringify"))
        assertTrue(script.contains("inputFound"))
        assertTrue(script.contains("submitted"))
    }

    @Test
    fun `전송 에스컬레이션 스크립트에 4단계 모달리티가 포함되어 있다`() {
        val script1 = ExternalAIScripts.submitPromptScript(DirectAIProvider.OPEN_AI, attemptNumber = 1)
        assertTrue(script1.contains("BUTTON_CLICK"))

        val script2 = ExternalAIScripts.submitPromptScript(DirectAIProvider.OPEN_AI, attemptNumber = 2)
        assertTrue(script2.contains("POINTER_TOUCH_CLICK"))

        val script3 = ExternalAIScripts.submitPromptScript(DirectAIProvider.OPEN_AI, attemptNumber = 3)
        assertTrue(script3.contains("FORM_REQUEST_SUBMIT"))

        val script4 = ExternalAIScripts.submitPromptScript(DirectAIProvider.OPEN_AI, attemptNumber = 4)
        assertTrue(script4.contains("ENTER_KEY_EVENT"))
    }

    @Test
    fun `전송 검증 스크립트에 입력 비워짐, 메시지 수 증가, 생성 인디케이터 확인이 포함되어 있다`() {
        val script = ExternalAIScripts.verifySubmissionScript(DirectAIProvider.CLAUDE, baselineCount = 2)
        assertTrue(script.contains("inputCleared"))
        assertTrue(script.contains("countIncreased"))
        assertTrue(script.contains("isGeneratingVisible"))
        assertTrue(script.contains("submitted"))
    }

    @Test
    fun `답변 추출 스크립트에 기준선 검증 및 pre code 블록 우선 추출이 포함되어 있다`() {
        val script = ExternalAIScripts.extractAnswerScript(DirectAIProvider.OPEN_AI)
        assertTrue(script.contains("window.__sm_ai_baseline"))
        assertTrue(script.contains("newAnswer"))
        assertTrue(script.contains("generating"))
        assertTrue(script.contains("pre code"))
        assertTrue(script.contains("JSON.stringify"))
    }

    @Test
    fun `독립 기준선 기록 스크립트가 유효하게 정의되어 있다`() {
        val script = ExternalAIScripts.recordBaselineScript(DirectAIProvider.GEMINI)
        assertTrue(script.contains("window.__sm_ai_baseline"))
        assertTrue(script.contains("JSON.stringify"))
        assertTrue(script.contains("isLoggedIn"))
        assertTrue(script.contains("hasChallenge"))
    }

    @Test
    fun `JSON 파싱 헬퍼는 evaluateJavascript 반환 형식의 주입 결과를 안전하게 디코딩한다`() {
        // 1. 정상 주입 성공
        val rawSuccess = "\"{\\\"success\\\":true,\\\"inputFound\\\":true,\\\"submitted\\\":false}\""
        val successRes = ExternalAIScripts.parseInjectionResult(rawSuccess)
        assertTrue(successRes.success)
        assertTrue(successRes.inputFound)
        assertFalse(successRes.submitted)
        assertNull(successRes.error)

        // 2. 입력창 미발견
        val rawNotFound = "\"{\\\"success\\\":false,\\\"inputFound\\\":false,\\\"submitted\\\":false,\\\"error\\\":\\\"INPUT_NOT_FOUND\\\"}\""
        val notFoundRes = ExternalAIScripts.parseInjectionResult(rawNotFound)
        assertFalse(notFoundRes.success)
        assertFalse(notFoundRes.inputFound)
        assertFalse(notFoundRes.submitted)
        assertEquals("INPUT_NOT_FOUND", notFoundRes.error)

        // 3. null 또는 깨진 문자열
        val nullRes = ExternalAIScripts.parseInjectionResult(null)
        assertFalse(nullRes.success)
        val malformedRes = ExternalAIScripts.parseInjectionResult("{invalid-json")
        assertFalse(malformedRes.success)
    }

    @Test
    fun `JSON 파싱 헬퍼는 evaluateJavascript 반환 형식의 폴링 결과를 안전하게 디코딩한다`() {
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

        val rawGenerating = "\"{\\\"newAnswer\\\":true,\\\"generating\\\":true,\\\"text\\\":\\\"답변 작성 중...\\\"}\""
        val genRes = ExternalAIScripts.parsePollResult(rawGenerating)
        assertTrue(genRes.newAnswer)
        assertTrue(genRes.generating)
        assertEquals("답변 작성 중...", genRes.text)

        val nullRes = ExternalAIScripts.parsePollResult(null)
        assertFalse(nullRes.newAnswer)
        assertFalse(nullRes.generating)
        assertEquals("", nullRes.text)
    }

    @Test
    fun `안정성 리듀서는 새 답변이 없거나 생성 중일 때 카운터를 즉시 리셋한다`() {
        val initial = ExternalAIStabilityState()

        val noAnswerPoll = ExternalAIPollResult(newAnswer = false, generating = false, text = "")
        val state1 = ExternalAIStabilityReducer.step(initial, noAnswerPoll)
        assertEquals(0, state1.consecutiveMatches)
        assertFalse(state1.isStable)
        assertNull(state1.stableAnswer)

        val generatingPoll = ExternalAIPollResult(
            newAnswer = true,
            generating = true,
            text = "#해시태그 #긴텍스트본문"
        )
        val state2 = ExternalAIStabilityReducer.step(initial, generatingPoll)
        assertEquals(0, state2.consecutiveMatches)
        assertFalse(state2.isStable)
        assertNull(state2.stableAnswer)
    }

    @Test
    fun `안정성 리듀서는 공백만 있는 답변에 대해 안정 상태로 진입하지 않는다`() {
        val blankPoll = ExternalAIPollResult(
            newAnswer = true,
            generating = false,
            text = "   \n\t  "
        )
        var state = ExternalAIStabilityState()
        for (i in 1..5) {
            state = ExternalAIStabilityReducer.step(state, blankPoll)
            assertFalse(state.isStable)
            assertNull(state.stableAnswer)
            assertEquals(0, state.consecutiveMatches)
        }
    }

    @Test
    fun `안정성 리듀서는 3회 연속 동일 답변이 관측될 때만 COMPLETED 안정 상태로 전환된다 (임의 최소 길이 없음)`() {
        val sampleText = "#기록 #하루\n좋은 하루입니다."
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
            text = sampleText + "\n추가 문장"
        )
        state = ExternalAIStabilityReducer.step(state, changedPoll)
        assertEquals(1, state.consecutiveMatches)
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

    @Test
    fun `AIChoice 항목들은 직접 실행 대상과 1대1 매핑된다`() {
        val choices = AIChoice.all
        assertEquals(4, choices.size)

        // Gemini
        val geminiChoice = choices[0] as AIChoice.External
        assertEquals(DirectAIProvider.GEMINI, geminiChoice.provider)

        // ChatGPT
        val chatGptChoice = choices[1] as AIChoice.External
        assertEquals(DirectAIProvider.OPEN_AI, chatGptChoice.provider)

        // Claude
        val claudeChoice = choices[2] as AIChoice.External
        assertEquals(DirectAIProvider.CLAUDE, claudeChoice.provider)

        // OnDevice
        val onDeviceChoice = choices[3]
        assertTrue(onDeviceChoice is AIChoice.OnDevice)
    }

    @Test
    fun `스타일 요약 문구 형식이 규격과 정확히 일치한다`() {
        val destination = com.armsone.starmanager.model.PostDestination.INSTAGRAM
        val mood = com.armsone.starmanager.model.PostMood.WITTY
        val style = com.armsone.starmanager.model.PostStyle.MEMO
        val tone = com.armsone.starmanager.model.PostTone.KIND
        val characterCount = 200

        val formatted = "${destination.title} · ${mood.rawValue} · ${style.title} · ${tone.title} · 목표 ${characterCount}자"
        assertEquals("Instagram · 재치 있게 · 메모 · 친절하게 · 목표 200자", formatted)
    }

    // MARK: - AIBI 로그인 관리 및 인증 상태 프로브 계약 테스트

    @Test
    fun `외부 AI 인증 상태 표기 문구가 규격과 정확히 일치한다`() {
        assertEquals("확인 중", com.armsone.starmanager.ui.externalai.ExternalAIAuthState.CHECKING.label)
        assertEquals("로그인됨", com.armsone.starmanager.ui.externalai.ExternalAIAuthState.LOGGED_IN.label)
        assertEquals("로그인 필요", com.armsone.starmanager.ui.externalai.ExternalAIAuthState.REQUIRES_LOGIN.label)

        assertTrue(com.armsone.starmanager.ui.externalai.ExternalAIAuthState.CHECKING.isChecking)
        assertTrue(com.armsone.starmanager.ui.externalai.ExternalAIAuthState.LOGGED_IN.isLoggedIn)
        assertTrue(com.armsone.starmanager.ui.externalai.ExternalAIAuthState.REQUIRES_LOGIN.requiresLogin)
    }

    @Test
    fun `인증 상태 확인 스크립트 계약에 긍정적 인증 증거 검사와 챌린지 검사가 포함되어 있다`() {
        val providers = listOf(DirectAIProvider.GEMINI, DirectAIProvider.OPEN_AI, DirectAIProvider.CLAUDE, DirectAIProvider.GROK)
        for (provider in providers) {
            val script = ExternalAIScripts.checkAuthStatusScript(provider)
            assertTrue(script.contains("authMarkerSelectors"))
            assertTrue(script.contains("hasPositiveEvidence"))
            assertTrue(script.contains("AUTHENTICATED"))
            assertTrue(script.contains("SECURITY_CHALLENGE_PRESENTED"))
            assertTrue(script.contains("LOGIN_REQUIRED"))
            assertTrue(script.contains("NO_POSITIVE_EVIDENCE"))
        }

        val openAIScript = ExternalAIScripts.checkAuthStatusScript(DirectAIProvider.OPEN_AI)
        assertTrue(openAIScript.contains("#prompt-textarea"))
        assertTrue(openAIScript.contains("var requireVisible = true"))

        val backgroundOpenAIScript = ExternalAIScripts.checkAuthStatusScript(
            DirectAIProvider.OPEN_AI,
            requireVisible = false
        )
        assertTrue(backgroundOpenAIScript.contains("var requireVisible = false"))
        assertTrue(backgroundOpenAIScript.indexOf("hasPositiveEvidence") < backgroundOpenAIScript.indexOf("var loginEl"))

        val geminiScript = ExternalAIScripts.checkAuthStatusScript(DirectAIProvider.GEMINI)
        assertTrue(geminiScript.contains("ql-editor") || geminiScript.contains("rich-textarea"))

        val claudeScript = ExternalAIScripts.checkAuthStatusScript(DirectAIProvider.CLAUDE)
        assertTrue(claudeScript.contains("ProseMirror"))
    }

    @Test
    fun `JSON 파싱 헬퍼는 evaluateJavascript 반환 형식의 인증 확인 결과를 안전하게 디코딩한다`() {
        // 1. 긍정적 인증 성공 (컴포저/계정 마커 확인)
        val rawAuth = "{\"success\":true,\"authenticated\":true,\"hasInput\":true,\"hasLogin\":false,\"hasChallenge\":false,\"reason\":\"AUTHENTICATED\"}"
        val authRes = ExternalAIScripts.parseAuthCheckResult(rawAuth)
        assertTrue(authRes.success)
        assertTrue(authRes.authenticated)
        assertTrue(authRes.hasInput)
        assertFalse(authRes.hasLogin)
        assertFalse(authRes.hasChallenge)
        assertEquals("AUTHENTICATED", authRes.reason)

        // 2. evaluateJavascript의 이중 문자열 래핑 형태
        val rawWrapped = "\"{\\\"success\\\":true,\\\"authenticated\\\":true,\\\"hasInput\\\":true,\\\"hasLogin\\\":false,\\\"hasChallenge\\\":false,\\\"reason\\\":\\\"AUTHENTICATED\\\"}\""
        val wrappedRes = ExternalAIScripts.parseAuthCheckResult(rawWrapped)
        assertTrue(wrappedRes.success)
        assertTrue(wrappedRes.authenticated)

        // 3. 로그인 요구 상태
        val rawLoginReq = "{\"success\":true,\"authenticated\":false,\"hasInput\":false,\"hasLogin\":true,\"hasChallenge\":false,\"reason\":\"LOGIN_REQUIRED\"}"
        val loginRes = ExternalAIScripts.parseAuthCheckResult(rawLoginReq)
        assertTrue(loginRes.success)
        assertFalse(loginRes.authenticated)
        assertTrue(loginRes.hasLogin)
        assertEquals("LOGIN_REQUIRED", loginRes.reason)

        // 4. 보안 챌린지 상태
        val rawChallenge = "{\"success\":true,\"authenticated\":false,\"hasInput\":false,\"hasLogin\":false,\"hasChallenge\":true,\"reason\":\"SECURITY_CHALLENGE_PRESENTED\"}"
        val chRes = ExternalAIScripts.parseAuthCheckResult(rawChallenge)
        assertTrue(chRes.success)
        assertFalse(chRes.authenticated)
        assertTrue(chRes.hasChallenge)
        assertEquals("SECURITY_CHALLENGE_PRESENTED", chRes.reason)

        // 5. 긍정 증거 부재 (하이드레이션 중)
        val rawNoEvidence = "{\"success\":true,\"authenticated\":false,\"hasInput\":false,\"hasLogin\":false,\"hasChallenge\":false,\"reason\":\"NO_POSITIVE_EVIDENCE\"}"
        val noEvRes = ExternalAIScripts.parseAuthCheckResult(rawNoEvidence)
        assertTrue(noEvRes.success)
        assertFalse(noEvRes.authenticated)
        assertFalse(noEvRes.hasLogin)
        assertFalse(noEvRes.hasChallenge)

        // 6. null, 빈 문자열, 비정상 응답
        val nullRes = ExternalAIScripts.parseAuthCheckResult(null)
        assertFalse(nullRes.authenticated)
        assertEquals("NO_RESPONSE", nullRes.reason)

        val invalidRes = ExternalAIScripts.parseAuthCheckResult("{malformed-json")
        assertFalse(invalidRes.authenticated)
    }
}
