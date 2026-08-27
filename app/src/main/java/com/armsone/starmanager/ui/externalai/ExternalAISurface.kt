package com.armsone.starmanager.ui.externalai

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExternalAISurface(
    provider: DirectAIProvider,
    mode: ExternalAISurfaceMode,
    prompt: String = "",
    onClose: () -> Unit,
    onImport: (String) -> Unit = {},
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentStatus by remember {
        mutableStateOf(if (mode.isLogin) ExternalAIStatus.CONNECTING else ExternalAIStatus.CONNECTING)
    }
    var statusDetail by remember { mutableStateOf<String?>(null) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var extractedAnswer by remember { mutableStateOf<String?>(null) }
    var isPollingAnswer by remember { mutableStateOf(false) }
    var stabilityState by remember { mutableStateOf(ExternalAIStabilityState()) }

    fun copyPromptToClipboard() {
        if (prompt.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("StarManagerPrompt", prompt))
            statusDetail = "요청문이 클립보드에 복사되었어요."
        }
    }

    fun executePromptInjection(wv: WebView) {
        if (mode.isLogin || prompt.isBlank()) return
        stabilityState = stabilityState.reset()
        extractedAnswer = null
        isPollingAnswer = false
        currentStatus = ExternalAIStatus.WAITING_FOR_INPUT
        statusDetail = "입력창을 찾아 요청문을 입력하는 중…"

        val script = ExternalAIScripts.injectPromptScript(provider, prompt)
        wv.evaluateJavascript(script) { result ->
            val injection = ExternalAIScripts.parseInjectionResult(result)
            if (!injection.inputFound) {
                currentStatus = ExternalAIStatus.WAITING_FOR_INPUT
                statusDetail = "입력창을 찾는 중입니다. [다시 넣기]를 눌러보세요."
                isPollingAnswer = false
            } else if (!injection.submitted) {
                currentStatus = ExternalAIStatus.INPUT_READY
                statusDetail = "자동 전송에 실패했습니다. 화면의 전송(Send) 버튼을 직접 눌러주세요."
                isPollingAnswer = false
            } else {
                currentStatus = ExternalAIStatus.GENERATING
                statusDetail = "요청문이 전송되었어요. 답변 생성을 기다리는 중…"
                isPollingAnswer = true
            }
        }
    }

    fun pollForAnswer(wv: WebView) {
        if (mode.isLogin) return
        val script = ExternalAIScripts.extractAnswerScript(provider)
        wv.evaluateJavascript(script) { result ->
            val pollResult = ExternalAIScripts.parsePollResult(result)
            val nextStability = ExternalAIStabilityReducer.step(stabilityState, pollResult)
            stabilityState = nextStability

            if (pollResult.generating) {
                currentStatus = ExternalAIStatus.GENERATING
                statusDetail = "답변 생성 중…"
            } else if (nextStability.isStable && nextStability.stableAnswer != null) {
                val stableText = nextStability.stableAnswer
                extractedAnswer = stableText
                currentStatus = ExternalAIStatus.COMPLETED
                statusDetail = "답변 생성 완료 (${stableText.length}자)"
                isPollingAnswer = false
            } else if (pollResult.newAnswer && pollResult.text.isNotBlank()) {
                currentStatus = ExternalAIStatus.GENERATING
                statusDetail = "답변 수신 중… (${nextStability.consecutiveMatches}/${ExternalAIStabilityState.REQUIRED_STABLE_POLLS})"
            }
        }
    }

    // 주기적 생성 결과 확인
    LaunchedEffect(isPollingAnswer) {
        if (isPollingAnswer) {
            for (i in 0..60) {
                delay(2000)
                if (!isPollingAnswer) break
                webViewRef?.let { pollForAnswer(it) }
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
                        if (mode.isLogin) "${provider.title} 로그인" else provider.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTheme.labelPrimary(appearance),
                        maxLines = 1
                    )
                    Text(
                        if (mode.isLogin) "공식 로그인 페이지" else currentStatus.message,
                        fontSize = 12.sp,
                        color = BrandTheme.labelSecondary(appearance),
                        maxLines = 1
                    )
                }

                if (!mode.isLogin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionButton(
                            title = "다시 넣기",
                            icon = Icons.Filled.Refresh,
                            testTag = "externalai.reinject",
                            appearance = appearance,
                            onClick = { webViewRef?.let { executePromptInjection(it) } }
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

            // 안내/상태 바 (생성 모드 또는 복사 알림 시)
            AnimatedVisibility(visible = statusDetail != null || extractedAnswer != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(BrandTheme.accent.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        statusDetail ?: "답변이 준비되었습니다.",
                        fontSize = 13.sp,
                        color = BrandTheme.labelPrimary(appearance),
                        modifier = Modifier.weight(1f)
                    )

                    val answer = extractedAnswer
                    if (answer != null && !mode.isLogin) {
                        Spacer(Modifier.width(8.dp))
                        Row(
                            Modifier
                                .background(BrandTheme.accent, RoundedCornerShape(8.dp))
                                .clickable {
                                    onImport(answer)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("externalai.import"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("가져오기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
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

                            // 쿠키 영속화 설정
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val targetUrl = request?.url?.toString()
                                    if (ExternalAISecurityPolicy.isAllowedUrl(targetUrl, provider)) {
                                        return false
                                    }
                                    return true
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    currentStatus = ExternalAIStatus.CONNECTING
                                    statusDetail = null
                                    extractedAnswer = null
                                    isPollingAnswer = false
                                    stabilityState = stabilityState.reset()
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    cookieManager.flush()
                                    extractedAnswer = null
                                    stabilityState = stabilityState.reset()
                                    if (!mode.isLogin && prompt.isNotBlank()) {
                                        scope.launch {
                                            delay(1200)
                                            view?.let { executePromptInjection(it) }
                                        }
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
