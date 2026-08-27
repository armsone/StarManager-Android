package com.armsone.starmanager.ui.externalai

import com.armsone.starmanager.service.DirectAIProvider
import java.net.URI

object ExternalAISecurityPolicy {

    private val commonAuthAndCdnHosts = listOf(
        "accounts.google.com",
        "myaccount.google.com",
        "appleid.apple.com",
        "login.microsoftonline.com",
        "login.live.com",
        "challenges.cloudflare.com",
        "recaptcha.net",
        "www.recaptcha.net"
    )

    private val geminiAllowedHostSuffixes = listOf(
        "google.com",
        "google.co.kr",
        "gstatic.com",
        "googleusercontent.com"
    )

    private val chatGptAllowedHostSuffixes = listOf(
        "chatgpt.com",
        "openai.com",
        "oaistatic.com",
        "oaiusercontent.com",
        "auth0.com"
    )

    private val claudeAllowedHostSuffixes = listOf(
        "claude.ai",
        "anthropic.com",
        "claudecdn.com"
    )

    private val grokAllowedHostSuffixes = listOf(
        "grok.com",
        "x.ai",
        "x.com",
        "twitter.com"
    )

    fun isAllowedUrl(url: String?, provider: DirectAIProvider): Boolean {
        if (url.isNullOrBlank()) return false
        if (url == "about:blank") return true

        val parsed = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase() ?: return false
        if (scheme != "https") return false

        val host = parsed.host?.lowercase() ?: return false
        if (host.isEmpty()) return false

        // 공통 인증 및 캡차/CDN 도메인 검사
        if (commonAuthAndCdnHosts.any { host == it || host.endsWith(".$it") }) {
            return true
        }

        // 제공사별 허용 호스트 검사
        val providerSuffixes = when (provider) {
            DirectAIProvider.GEMINI -> geminiAllowedHostSuffixes
            DirectAIProvider.OPEN_AI -> chatGptAllowedHostSuffixes
            DirectAIProvider.CLAUDE -> claudeAllowedHostSuffixes
            DirectAIProvider.GROK -> grokAllowedHostSuffixes
        }

        return providerSuffixes.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }
}
