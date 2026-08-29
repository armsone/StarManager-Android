package com.armsone.starmanager

import com.armsone.starmanager.service.DirectAIProvider
import com.armsone.starmanager.ui.automation.AutomationProviderSelector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationProviderSelectorTest {

    @Test
    fun `무작위 선택은 Grok을 제외한 세 곳 중에서만 고른다`() {
        repeat(50) {
            val provider = AutomationProviderSelector.selectRandom()
            assertTrue(provider in AutomationProviderSelector.ELIGIBLE_PROVIDERS)
            assertFalse(provider == DirectAIProvider.GROK)
        }
    }

    @Test
    fun `제외 대상을 지정하면 남은 두 곳 중에서만 고른다`() {
        repeat(50) {
            val providerGeminiExcluded = AutomationProviderSelector.selectRandom(exclude = DirectAIProvider.GEMINI)
            assertFalse(providerGeminiExcluded == DirectAIProvider.GEMINI)
            assertFalse(providerGeminiExcluded == DirectAIProvider.GROK)
            assertTrue(providerGeminiExcluded == DirectAIProvider.OPEN_AI || providerGeminiExcluded == DirectAIProvider.CLAUDE)

            val providerOpenAiExcluded = AutomationProviderSelector.selectRandom(exclude = DirectAIProvider.OPEN_AI)
            assertFalse(providerOpenAiExcluded == DirectAIProvider.OPEN_AI)
            assertFalse(providerOpenAiExcluded == DirectAIProvider.GROK)
            assertTrue(providerOpenAiExcluded == DirectAIProvider.GEMINI || providerOpenAiExcluded == DirectAIProvider.CLAUDE)

            val providerClaudeExcluded = AutomationProviderSelector.selectRandom(exclude = DirectAIProvider.CLAUDE)
            assertFalse(providerClaudeExcluded == DirectAIProvider.CLAUDE)
            assertFalse(providerClaudeExcluded == DirectAIProvider.GROK)
            assertTrue(providerClaudeExcluded == DirectAIProvider.GEMINI || providerClaudeExcluded == DirectAIProvider.OPEN_AI)
        }
    }

    @Test
    fun `AutomationSessionState 모델 상태 계약이 유효하다`() {
        val idle = com.armsone.starmanager.ui.automation.AutomationSessionState.Idle
        assertTrue(idle is com.armsone.starmanager.ui.automation.AutomationSessionState)

        val processing = com.armsone.starmanager.ui.automation.AutomationSessionState.Processing(
            provider = DirectAIProvider.GEMINI,
            stepTitle = "사진을 준비하는 중…",
            stepSubtitle = "선택한 사진 5장을 전송용으로 줄이고 있어요",
            elapsedSeconds = 10L,
            attachments = emptyList(),
            rawImages = emptyList(),
            requestId = 1
        )
        org.junit.Assert.assertEquals(DirectAIProvider.GEMINI, processing.provider)
        org.junit.Assert.assertEquals(10L, processing.elapsedSeconds)
        org.junit.Assert.assertEquals(1, processing.requestId)

        val result = com.armsone.starmanager.ui.automation.AutomationSessionState.Result(
            provider = DirectAIProvider.OPEN_AI,
            generatedText = "완성된 글",
            attachments = emptyList(),
            rawImages = emptyList()
        )
        org.junit.Assert.assertEquals(DirectAIProvider.OPEN_AI, result.provider)
        org.junit.Assert.assertEquals("완성된 글", result.generatedText)
        assertFalse(result.isSharing)

        val failure = com.armsone.starmanager.ui.automation.AutomationSessionState.Failure(
            lastProvider = DirectAIProvider.CLAUDE,
            errorMessage = "에러 메시지",
            attachments = emptyList(),
            rawImages = emptyList()
        )
        org.junit.Assert.assertEquals(DirectAIProvider.CLAUDE, failure.lastProvider)
        org.junit.Assert.assertEquals("에러 메시지", failure.errorMessage)
    }
}
