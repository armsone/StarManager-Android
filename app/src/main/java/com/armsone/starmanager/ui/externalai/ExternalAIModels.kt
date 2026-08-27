package com.armsone.starmanager.ui.externalai

enum class ExternalAISurfaceMode {
    LOGIN,
    GENERATION;

    val isLogin: Boolean get() = this == LOGIN
}

enum class ExternalAIStatus(val message: String) {
    IDLE("대기 중"),
    CONNECTING("연결 중…"),
    WAITING_FOR_INPUT("입력창 찾는 중…"),
    INPUT_READY("요청문 입력 완료"),
    GENERATING("답변 생성 중…"),
    COMPLETED("생성 완료"),
    ERROR("확인 필요")
}

/** 백그라운드 브라우저 자동화 단계 */
enum class ExternalAIAutomationPhase {
    IDLE,
    CONNECTING,
    SUBMITTED,
    WAITING_ELAPSED,
    COMPLETED,
    FALLBACK_REQUIRED,
    ERROR
}

/** 대화형 웹뷰 폴백 사유 및 사용자 안내 배너 문구 */
enum class ExternalAIFallbackReason(val bannerText: String) {
    LOGIN_REQUIRED("로그인이 필요해요"),
    SECURITY_VERIFICATION("보안 확인이 필요해요"),
    MANUAL_INPUT_REQUIRED("입력창을 확인해 주세요"),
    MANUAL_CONFIRMATION("직접 확인이 필요해요")
}

data class ExternalAIDomErrorResult(
    val hasError: Boolean,
    val error: String? = null
)

data class ExternalAIInjectionResult(
    val success: Boolean,
    val inputFound: Boolean,
    val submitted: Boolean,
    val isAuthChallenge: Boolean = false,
    val error: String? = null
)

data class ExternalAIPollResult(
    val newAnswer: Boolean,
    val generating: Boolean,
    val text: String
)

data class ExternalAIStabilityState(
    val consecutiveMatches: Int = 0,
    val lastCleanedText: String = "",
    val isStable: Boolean = false,
    val stableAnswer: String? = null
) {
    fun reset(): ExternalAIStabilityState = ExternalAIStabilityState()

    companion object {
        const val REQUIRED_STABLE_POLLS = 3
        const val MIN_ANSWER_LENGTH = 15
    }
}

object ExternalAIStabilityReducer {
    fun step(
        currentState: ExternalAIStabilityState,
        pollResult: ExternalAIPollResult
    ): ExternalAIStabilityState {
        // 새 응답이 아니거나 생성/스트리밍 진행 중인 경우 안정성 카운터 초기화
        if (!pollResult.newAnswer || pollResult.generating) {
            val cleaned = if (pollResult.newAnswer) ExternalAIAnswerCleaner.clean(pollResult.text) else ""
            return currentState.copy(
                consecutiveMatches = 0,
                lastCleanedText = cleaned,
                isStable = false,
                stableAnswer = null
            )
        }

        val cleaned = ExternalAIAnswerCleaner.clean(pollResult.text)
        if (cleaned.isBlank() || cleaned.length < ExternalAIStabilityState.MIN_ANSWER_LENGTH) {
            return currentState.copy(
                consecutiveMatches = 0,
                lastCleanedText = cleaned,
                isStable = false,
                stableAnswer = null
            )
        }

        return if (cleaned == currentState.lastCleanedText) {
            val newCount = currentState.consecutiveMatches + 1
            val stable = newCount >= ExternalAIStabilityState.REQUIRED_STABLE_POLLS
            currentState.copy(
                consecutiveMatches = newCount,
                lastCleanedText = cleaned,
                isStable = stable,
                stableAnswer = if (stable) cleaned else null
            )
        } else {
            currentState.copy(
                consecutiveMatches = 1,
                lastCleanedText = cleaned,
                isStable = false,
                stableAnswer = null
            )
        }
    }
}
