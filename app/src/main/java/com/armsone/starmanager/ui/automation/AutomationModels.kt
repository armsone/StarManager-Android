package com.armsone.starmanager.ui.automation

import com.armsone.starmanager.service.DirectAIProvider
import com.armsone.starmanager.ui.externalai.ExternalAIAttachment

/**
 * Automation Studio 상태 모델 (iPhone 2.5.0 Automation Studio와 1:1 대응).
 * 무작위 AI 선정, 1~8장 정규화 이미지 처리, 119초 타임아웃, 전체화면 상태 제어를 담당한다.
 */
sealed class AutomationSessionState {
    data object Idle : AutomationSessionState()

    data class Processing(
        val provider: DirectAIProvider,
        val stepTitle: String,
        val stepSubtitle: String,
        val elapsedSeconds: Long = 0L,
        val attachments: List<ExternalAIAttachment> = emptyList(),
        val rawImages: List<ByteArray> = emptyList(),
        val requestId: Int = 0
    ) : AutomationSessionState()

    data class Result(
        val provider: DirectAIProvider,
        val generatedText: String,
        val attachments: List<ExternalAIAttachment>,
        val rawImages: List<ByteArray>,
        val isSharing: Boolean = false,
        val shareMessage: String? = null
    ) : AutomationSessionState()

    data class Failure(
        val lastProvider: DirectAIProvider,
        val errorMessage: String,
        val attachments: List<ExternalAIAttachment>,
        val rawImages: List<ByteArray>
    ) : AutomationSessionState()
}

/**
 * 무작위 자동화 제공사 선택기.
 * Gemini, ChatGPT, Claude만 포함하며 Grok은 무작위 자동화에서 제외된다.
 */
object AutomationProviderSelector {
    val ELIGIBLE_PROVIDERS = listOf(
        DirectAIProvider.GEMINI,
        DirectAIProvider.OPEN_AI,
        DirectAIProvider.CLAUDE
    )

    fun selectRandom(exclude: DirectAIProvider? = null): DirectAIProvider {
        val candidates = if (exclude != null) {
            ELIGIBLE_PROVIDERS.filter { it != exclude }.ifEmpty { ELIGIBLE_PROVIDERS }
        } else {
            ELIGIBLE_PROVIDERS
        }
        return candidates.random()
    }
}
