package com.armsone.starmanager.ui.externalai

/**
 * 백그라운드 브라우저 자동화 도중 사용자 개입(폴백) 필요성을 판별하는 분류기.
 */
object ExternalAIFallbackClassifier {

    fun classifyUrl(url: String?): ExternalAIFallbackReason? {
        if (url.isNullOrBlank()) return null
        val lower = url.lowercase()
        return when {
            lower.contains("accounts.google.com") ||
                lower.contains("auth0.openai.com") ||
                lower.contains("appleid.apple.com") ||
                lower.contains("login.microsoftonline.com") ||
                lower.contains("/login") ||
                lower.contains("/signin") ||
                lower.contains("/auth") ||
                lower.contains("/u/login") -> ExternalAIFallbackReason.LOGIN_REQUIRED

            lower.contains("challenges.cloudflare.com") ||
                lower.contains("recaptcha") ||
                lower.contains("turnstile") ||
                lower.contains("/challenge") -> ExternalAIFallbackReason.SECURITY_VERIFICATION

            else -> null
        }
    }

    fun classifyDomState(
        isAuthRequired: Boolean,
        isSecurityChallenge: Boolean,
        inputFound: Boolean,
        submitted: Boolean
    ): ExternalAIFallbackReason? {
        return when {
            isAuthRequired -> ExternalAIFallbackReason.LOGIN_REQUIRED
            isSecurityChallenge -> ExternalAIFallbackReason.SECURITY_VERIFICATION
            !inputFound -> ExternalAIFallbackReason.MANUAL_INPUT_REQUIRED
            !submitted -> ExternalAIFallbackReason.MANUAL_CONFIRMATION
            else -> null
        }
    }
}
