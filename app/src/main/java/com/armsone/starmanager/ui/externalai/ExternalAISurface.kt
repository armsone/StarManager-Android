package com.armsone.starmanager.ui.externalai

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.InputDevice
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.armsone.starmanager.design.BrandTheme
import com.armsone.starmanager.design.LocalAppAppearance
import com.armsone.starmanager.model.AppAppearance
import com.armsone.starmanager.service.DirectAIProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExternalAISurface(
    provider: DirectAIProvider,
    mode: ExternalAISurfaceMode,
    prompt: String = "",
    fallbackReason: ExternalAIFallbackReason? = null,
    onClose: () -> Unit,
    onLoginSuccess: () -> Unit = {},
    onImport: (String) -> Unit = {},
    onSubmitted: () -> Unit = {},
    onError: (String?) -> Unit = {},
    autoImportOnComplete: Boolean = true,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var activeFallbackReason by remember(fallbackReason) { mutableStateOf(fallbackReason) }
    var detectedFallbackReason by remember { mutableStateOf<ExternalAIFallbackReason?>(null) }
    val effectiveFallbackReason = activeFallbackReason ?: detectedFallbackReason
    var hasClosedOnAuth by remember { mutableStateOf(false) }

    fun handlePositiveAuthSuccess() {
        if (hasClosedOnAuth) return
        hasClosedOnAuth = true
        activeFallbackReason = null
        detectedFallbackReason = null
        CookieManager.getInstance().flush()
        onLoginSuccess()
        onClose()
    }

    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isPageReady by remember { mutableStateOf(false) }
    var isAutoFilling by remember { mutableStateOf(false) }
    var hasFilledPrompt by remember { mutableStateOf(false) }
    var fillFailed by remember { mutableStateOf(false) }
    var hasSubmittedPrompt by remember { mutableStateOf(false) }
    var submitFailed by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var latestAnswer by remember { mutableStateOf("") }
    var extractedAnswer by remember { mutableStateOf<String?>(null) }
    var hasImportedAnswer by remember { mutableStateOf(false) }
    var detectedErrorMessage by remember { mutableStateOf<String?>(null) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var stabilityState by remember { mutableStateOf(ExternalAIStabilityState()) }
    var navigationGeneration by remember { mutableStateOf(0) }

    fun copyPromptToClipboard() {
        if (prompt.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("StarManager Prompt", prompt))
        }
    }

    fun importAnswer(text: String) {
        if (hasImportedAnswer) return
        val cleaned = ExternalAIAnswerCleaner.clean(text, provider)
        if (cleaned.isBlank()) return
        hasImportedAnswer = true
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("StarManager Result", cleaned))
        onImport(cleaned)
        onClose()
    }

    // 자동 제출 시도 (15초 루프, 500ms 주기 + 700ms 검증)
    suspend fun submitPromptWhenReady(wv: WebView, baselineCount: Int): Boolean {
        if (hasSubmittedPrompt) return true
        submitFailed = false
        val startTime = System.currentTimeMillis()
        var attempt = 1

        while (System.currentTimeMillis() - startTime < 15_000L && !hasSubmittedPrompt && !hasImportedAnswer) {
            // 일부 제공자는 JavaScript click()의 비신뢰 이벤트를 거부한다. 첫 시도는 실제 WebView 터치로 보낸다.
            if (attempt == 1) {
                val targetResult = suspendCancellableCoroutine<String?> { cont ->
                    wv.evaluateJavascript(ExternalAIScripts.submitTargetScript(provider)) { cont.resume(it) }
                }
                val target = ExternalAIScripts.parseSubmitPoint(targetResult)
                if (target != null) {
                    val x = target.first * wv.width
                    val y = target.second * wv.height
                    val downTime = SystemClock.uptimeMillis()
                    MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0).also {
                        it.source = InputDevice.SOURCE_TOUCHSCREEN
                        wv.dispatchTouchEvent(it)
                        it.recycle()
                    }
                    delay(80L)
                    MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0).also {
                        it.source = InputDevice.SOURCE_TOUCHSCREEN
                        wv.dispatchTouchEvent(it)
                        it.recycle()
                    }
                } else {
                    wv.evaluateJavascript(ExternalAIScripts.submitPromptScript(provider, attempt), null)
                }
            } else if (attempt == 2) {
                // Claude 등 비신뢰 click()을 거부하는 편집기는 실제 Enter 키 이벤트로 전송한다.
                wv.evaluateJavascript(ExternalAIScripts.focusInputScript(provider), null)
                wv.requestFocus()
                delay(80L)
                wv.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                wv.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            } else {
                wv.evaluateJavascript(ExternalAIScripts.submitPromptScript(provider, attempt), null)
            }

            // 검증 지연 700ms
            delay(700L)

            // 전송 검증
            val verifyScript = ExternalAIScripts.verifySubmissionScript(provider, baselineCount)
            val verifyResult = suspendCancellableCoroutine<String?> { cont ->
                wv.evaluateJavascript(verifyScript) { cont.resume(it) }
            }

            if (ExternalAIScripts.parseSubmissionVerified(verifyResult)) {
                hasSubmittedPrompt = true
                isGenerating = true
                onSubmitted()
                return true
            }

            attempt++
            delay(500L)
        }

        if (!hasSubmittedPrompt) {
            submitFailed = true
        }
        return hasSubmittedPrompt
    }

    // 프롬프트 입력 시도 (force=false: 사용자 입력 텍스트 보호)
    suspend fun fillPrompt(wv: WebView, force: Boolean): Boolean {
        val script = ExternalAIScripts.injectPromptScript(provider, prompt, force = force)
        val result = suspendCancellableCoroutine<String?> { cont ->
            wv.evaluateJavascript(script) { cont.resume(it) }
        }
        val injection = ExternalAIScripts.parseInjectionResult(result)
        return if (injection.success && injection.inputFound) {
            hasFilledPrompt = true
            fillFailed = false
            // focus()로 열린 IME가 제공사 전송 버튼을 가리지 않도록 입력 반영 후 즉시 정리한다.
            wv.evaluateJavascript("if(document.activeElement){document.activeElement.blur();}", null)
            wv.clearFocus()
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(wv.windowToken, 0)
            if (activeFallbackReason == ExternalAIFallbackReason.MANUAL_INPUT_REQUIRED) {
                activeFallbackReason = null
            }
            if (detectedFallbackReason == ExternalAIFallbackReason.MANUAL_INPUT_REQUIRED) {
                detectedFallbackReason = null
            }
            true
        } else {
            if (force) fillFailed = true
            false
        }
    }

    // LOGIN 모드: 내비게이션 완료 및 늦은 하이드레이션 중 긍정적 인증 상태를 감지하여 자동 닫기
    LaunchedEffect(mode, navigationGeneration, isPageReady) {
        if (!mode.isLogin || hasClosedOnAuth || !isPageReady) return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect

        val currentUrl = wv.url
        if (!ExternalAISecurityPolicy.canInjectScript(currentUrl, provider)) {
            // 인증/로그인 오리진이나 챌린지 오리진에서는 스크립트를 주입하지 않고 브라우저를 열어둔다.
            return@LaunchedEffect
        }

        // 스크립트 오리진으로 진입 시 기존 URL 기반 LOGIN_REQUIRED 잔여 상태 정리
        if (detectedFallbackReason == ExternalAIFallbackReason.LOGIN_REQUIRED) {
            detectedFallbackReason = null
        }
        if (activeFallbackReason == ExternalAIFallbackReason.LOGIN_REQUIRED) {
            activeFallbackReason = null
        }

        val startTime = System.currentTimeMillis()
        while (isActive && !hasClosedOnAuth && System.currentTimeMillis() - startTime < 45_000L) {
            val checkUrl = wv.url
            if (!ExternalAISecurityPolicy.canInjectScript(checkUrl, provider)) {
                break
            }

            val authScript = ExternalAIScripts.checkAuthStatusScript(provider)
            val authRes = suspendCancellableCoroutine<String?> { cont ->
                wv.evaluateJavascript(authScript) { cont.resume(it) }
            }

            val check = ExternalAIScripts.parseAuthCheckResult(authRes)
            if (check.authenticated) {
                handlePositiveAuthSuccess()
                return@LaunchedEffect
            } else if (check.hasChallenge) {
                detectedFallbackReason = ExternalAIFallbackReason.SECURITY_VERIFICATION
            } else if (check.hasLogin) {
                detectedFallbackReason = ExternalAIFallbackReason.LOGIN_REQUIRED
            } else {
                if (detectedFallbackReason == ExternalAIFallbackReason.LOGIN_REQUIRED) {
                    detectedFallbackReason = null
                }
            }

            delay(500L)
        }
    }

    // 메인 프레임 내비게이션 완료 시 45초 자동 채우기 및 전송 루프 실행
    LaunchedEffect(navigationGeneration) {
        if (mode.isLogin || prompt.isBlank() || hasSubmittedPrompt || hasImportedAnswer) return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect
        if (!ExternalAISecurityPolicy.canInjectScript(wv.url, provider)) return@LaunchedEffect
        isAutoFilling = true
        fillFailed = false
        val startTime = System.currentTimeMillis()
        var baselineCaptured = false
        var baselineCount = 0

        while (isActive && System.currentTimeMillis() - startTime < 45_000L && !hasSubmittedPrompt && !hasImportedAnswer) {
            if (isPageReady) {
                // 새 답변 판정 기준선은 작업당 한 번만 저장한다. 재시도 중 덮어쓰면 완성 답변을 놓친다.
                if (!baselineCaptured) {
                    val baselineScript = ExternalAIScripts.recordBaselineScript(provider)
                    val baselineRes = suspendCancellableCoroutine<String?> { cont ->
                        wv.evaluateJavascript(baselineScript) { cont.resume(it) }
                    }
                    baselineCount = ExternalAIScripts.parseBaselineCount(baselineRes)
                    baselineCaptured = true
                }

                if (fillPrompt(wv, force = false)) {
                    delay(350L)
                    if (submitPromptWhenReady(wv, baselineCount)) {
                        isAutoFilling = false
                        return@LaunchedEffect
                    }
                    // ChatGPT 지연 렌더링으로 입력창이 재교체된 경우 입력 완료 상태 되돌리고 재시도
                    hasFilledPrompt = false
                }
            }
            delay(700L)
        }

        isAutoFilling = false
        if (!hasSubmittedPrompt) {
            fillFailed = true
        }
    }

    // 답변 관찰 루프 (700ms 주기, 3회 연속 일치 관측 시 안정 판정)
    LaunchedEffect(hasSubmittedPrompt, navigationGeneration) {
        if (mode.isLogin || !hasSubmittedPrompt || hasImportedAnswer) return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect
        if (!ExternalAISecurityPolicy.canInjectScript(wv.url, provider)) return@LaunchedEffect
        stabilityState = stabilityState.reset()

        while (isActive && !hasImportedAnswer) {
            delay(700L)

            // 오류 감지
            val errorScript = ExternalAIScripts.extractErrorScript()
            val errorRes = suspendCancellableCoroutine<String?> { cont ->
                wv.evaluateJavascript(errorScript) { cont.resume(it) }
            }
            val domError = ExternalAIScripts.parseErrorResult(errorRes)
            if (domError.hasError && !domError.error.isNullOrBlank()) {
                val sanitized = ExternalAIErrorSanitizer.sanitize(domError.error, provider)
                detectedErrorMessage = sanitized
                isGenerating = false
                onError(sanitized)
                return@LaunchedEffect
            }

            // 답변 관찰
            val script = ExternalAIScripts.extractAnswerScript(provider)
            val answerRes = suspendCancellableCoroutine<String?> { cont ->
                wv.evaluateJavascript(script) { cont.resume(it) }
            }
            val poll = ExternalAIScripts.parsePollResult(answerRes)
            isGenerating = poll.generating

            if (poll.text.isNotBlank()) {
                latestAnswer = poll.text
            }

            val nextStability = ExternalAIStabilityReducer.step(stabilityState, poll)
            stabilityState = nextStability

            if (nextStability.isStable && nextStability.stableAnswer != null) {
                val stableText = nextStability.stableAnswer
                extractedAnswer = stableText
                if (autoImportOnComplete && !hasImportedAnswer) {
                    importAnswer(stableText)
                    return@LaunchedEffect
                }
            }
        }
    }

    // 남은 시간 역카운터 (전송 후 119초에서 자동 취소)
    LaunchedEffect(hasSubmittedPrompt, hasImportedAnswer) {
        if (hasSubmittedPrompt && !hasImportedAnswer) {
            elapsedSeconds = 0L
            while (isActive && !hasImportedAnswer) {
                delay(1000L)
                elapsedSeconds += 1L
                if (elapsedSeconds >= ExternalAITimerFormatter.GENERATION_TIMEOUT_SECONDS) {
                    val message = "1분 59초 동안 답변이 없어서 중단했어요. 다시 시도해 주세요."
                    detectedErrorMessage = message
                    onError(message)
                    onClose()
                    break
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler {
            val wv = webViewRef
            if (wv != null && wv.canGoBack()) {
                wv.goBack()
            } else {
                onClose()
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(BrandTheme.settingsBackground(appearance))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 상단 바
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(BrandTheme.settingsSectionBackground(appearance))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("externalai.close")
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = BrandTheme.labelPrimary(appearance)
                    )
                }

                Spacer(Modifier.width(4.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        if (mode.isLogin) "${provider.title} 로그인" else "${provider.title}에서 만들기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTheme.labelPrimary(appearance),
                        maxLines = 1
                    )
                    Text(
                        statusSubtitleText(
                            mode = mode,
                            detectedErrorMessage = detectedErrorMessage,
                            hasImportedAnswer = hasImportedAnswer,
                            isGenerating = isGenerating,
                            hasSubmittedPrompt = hasSubmittedPrompt,
                            latestAnswer = latestAnswer,
                            isPageReady = isPageReady,
                            hasFilledPrompt = hasFilledPrompt,
                            submitFailed = submitFailed,
                            isAutoFilling = isAutoFilling,
                            fillFailed = fillFailed,
                            elapsedSeconds = elapsedSeconds
                        ),
                        fontSize = 12.sp,
                        color = if (detectedErrorMessage != null) BrandTheme.red else BrandTheme.labelSecondary(appearance),
                        maxLines = 1
                    )
                }

                if (!mode.isLogin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (latestAnswer.isNotBlank() && !hasImportedAnswer) {
                            ActionButton(
                                title = "가져오기",
                                icon = Icons.Outlined.FileDownload,
                                testTag = "externalai.import",
                                appearance = appearance,
                                onClick = { importAnswer(latestAnswer) }
                            )
                        }
                        ActionButton(
                            title = "다시 넣기",
                            icon = Icons.Filled.Refresh,
                            testTag = "externalai.reinject",
                            appearance = appearance,
                            onClick = {
                                webViewRef?.let { wv ->
                                    scope.launch {
                                        hasSubmittedPrompt = false
                                        if (fillPrompt(wv, force = true)) {
                                            submitPromptWhenReady(wv, 0)
                                        }
                                    }
                                }
                            }
                        )
                        ActionButton(
                            title = "문구 복사",
                            icon = Icons.Filled.ContentCopy,
                            testTag = "externalai.copyPrompt",
                            appearance = appearance,
                            onClick = { copyPromptToClipboard() }
                        )
                    }
                }
            }

            if (pageProgress in 0.01f..0.99f) {
                LinearProgressIndicator(
                    progress = { pageProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = BrandTheme.accent
                )
            } else {
                HorizontalDivider(color = BrandTheme.divider(appearance))
            }

            if (!mode.isLogin && hasSubmittedPrompt && !hasImportedAnswer && detectedErrorMessage == null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(BrandTheme.settingsSectionBackground(appearance))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("답변을 기다리는 중", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            ExternalAITimerFormatter.formatWaitingStatus(elapsedSeconds),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { ExternalAITimerFormatter.progress(elapsedSeconds) },
                        modifier = Modifier.fillMaxWidth(),
                        color = BrandTheme.accent
                    )
                }
            }

            // 폴백 사유 배너 (iPhone fallbackBanner와 1:1 일치)
            AnimatedVisibility(visible = effectiveFallbackReason != null) {
                effectiveFallbackReason?.let { reason ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(BrandTheme.settingsSectionBackground(appearance))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("externalai.fallbackBanner"),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (reason) {
                                ExternalAIFallbackReason.LOGIN_REQUIRED -> Icons.Filled.Warning
                                ExternalAIFallbackReason.SECURITY_VERIFICATION -> Icons.Filled.Shield
                                else -> Icons.Filled.Warning
                            },
                            contentDescription = null,
                            tint = BrandTheme.accent,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                reason.bannerText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTheme.labelPrimary(appearance)
                            )
                            if (reason.detailText.isNotBlank()) {
                                Text(
                                    reason.detailText,
                                    fontSize = 12.sp,
                                    color = BrandTheme.labelSecondary(appearance)
                                )
                            }
                        }
                    }
                }
            }

            // 오류 배너
            AnimatedVisibility(visible = detectedErrorMessage != null) {
                detectedErrorMessage?.let { err ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(BrandTheme.red.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = BrandTheme.red, modifier = Modifier.size(16.dp))
                        Text(err, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandTheme.red, modifier = Modifier.weight(1f))
                    }
                }
            }

            // WebView 표면
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                userAgentString = userAgentString.replace("; wv", "")
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val targetUrl = request?.url?.toString()
                                    if (!ExternalAISecurityPolicy.isAllowedUrl(targetUrl, provider)) {
                                        return true
                                    }
                                    val urlReason = ExternalAIFallbackClassifier.classifyUrl(targetUrl)
                                    if (urlReason != null) {
                                        detectedFallbackReason = urlReason
                                    } else if (ExternalAISecurityPolicy.isScriptOrigin(targetUrl, provider)) {
                                        if (detectedFallbackReason == ExternalAIFallbackReason.LOGIN_REQUIRED) {
                                            detectedFallbackReason = null
                                        }
                                        if (activeFallbackReason == ExternalAIFallbackReason.LOGIN_REQUIRED) {
                                            activeFallbackReason = null
                                        }
                                    }
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isPageReady = false
                                    detectedErrorMessage = null
                                    val urlReason = ExternalAIFallbackClassifier.classifyUrl(url)
                                    if (urlReason != null) {
                                        detectedFallbackReason = urlReason
                                    } else if (ExternalAISecurityPolicy.isScriptOrigin(url, provider)) {
                                        if (detectedFallbackReason == ExternalAIFallbackReason.LOGIN_REQUIRED) {
                                            detectedFallbackReason = null
                                        }
                                        if (activeFallbackReason == ExternalAIFallbackReason.LOGIN_REQUIRED) {
                                            activeFallbackReason = null
                                        }
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    cookieManager.flush()
                                    isPageReady = true
                                    navigationGeneration += 1
                                    val urlReason = ExternalAIFallbackClassifier.classifyUrl(url)
                                    if (urlReason != null) {
                                        detectedFallbackReason = urlReason
                                    } else if (ExternalAISecurityPolicy.isScriptOrigin(url, provider)) {
                                        if (detectedFallbackReason == ExternalAIFallbackReason.LOGIN_REQUIRED) {
                                            detectedFallbackReason = null
                                        }
                                        if (activeFallbackReason == ExternalAIFallbackReason.LOGIN_REQUIRED) {
                                            activeFallbackReason = null
                                        }
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        val desc = error?.description?.toString() ?: "연결 실패"
                                        val sanitized = ExternalAIErrorSanitizer.sanitize("Network error: $desc", provider)
                                        detectedErrorMessage = sanitized
                                        onError(sanitized)
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageProgress = newProgress / 100f
                                }
                            }

                            loadUrl(provider.url)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("externalai.webview")
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                wv.stopLoading()
                wv.destroy()
            }
            webViewRef = null
        }
    }
}

private fun statusSubtitleText(
    mode: ExternalAISurfaceMode,
    detectedErrorMessage: String?,
    hasImportedAnswer: Boolean,
    isGenerating: Boolean,
    hasSubmittedPrompt: Boolean,
    latestAnswer: String,
    isPageReady: Boolean,
    hasFilledPrompt: Boolean,
    submitFailed: Boolean,
    isAutoFilling: Boolean,
    fillFailed: Boolean,
    elapsedSeconds: Long
): String {
    if (mode.isLogin) return "공식 로그인 페이지"
    if (detectedErrorMessage != null) return detectedErrorMessage
    if (hasImportedAnswer) return "답변을 가져왔어요"
    if (isGenerating || hasSubmittedPrompt) {
        return ExternalAITimerFormatter.formatWaitingStatus(elapsedSeconds)
    }
    if (latestAnswer.isNotBlank()) return "답변이 끝나가는 중이에요…"
    if (!isPageReady) return "페이지를 불러오는 중…"
    if (hasFilledPrompt) {
        if (submitFailed) return "자동 전송 실패 · 보내기 버튼을 눌러 주세요"
        return "자동으로 보내는 중…"
    }
    if (isAutoFilling) return "입력창에 자동으로 채우는 중…"
    if (fillFailed) return "채우기 실패 · ⋯ 메뉴에서 다시 넣기나 문구 복사를 써 주세요"
    return "입력창을 기다리는 중…"
}

@Composable
private fun ActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    appearance: AppAppearance,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(BrandTheme.accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = BrandTheme.accent, modifier = Modifier.size(14.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandTheme.accent)
    }
}
