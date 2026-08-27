package com.armsone.starmanager.ui.externalai

import com.armsone.starmanager.service.DirectAIProvider
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object ExternalAIScripts {

    private val gson = Gson()

    /** 프롬프트 문자열을 JavaScript 안전한 문자열 리터럴로 인코딩 */
    fun jsStringLiteral(value: String): String = gson.toJson(value)

    /** 현재 어시스턴트 메시지 기준선(baseline)을 페이지 JS 전역 상태에 기록하는 스크립트 */
    fun recordBaselineScript(): String {
        return """
            (function() {
                try {
                    var selectorGroups = [
                        'div[data-message-author-role="assistant"]',
                        'div.model-response-text, model-response, message-content',
                        'div.font-claude-message, div[data-testid="chat-message-assistant"], div[class*="font-claude"]',
                        'div[class*="assistant"]',
                        'div[data-is-streaming]',
                        '.markdown.prose'
                    ];
                    var assistantEls = [];
                    for (var i = 0; i < selectorGroups.length; i++) {
                        var els = document.querySelectorAll(selectorGroups[i]);
                        if (els && els.length > 0) {
                            assistantEls = els;
                            break;
                        }
                    }
                    var count = assistantEls.length;
                    var lastText = '';
                    var lastId = '';
                    if (count > 0) {
                        var lastEl = assistantEls[count - 1];
                        lastText = (lastEl.innerText || lastEl.textContent || '').trim();
                        lastId = lastEl.getAttribute('data-message-id') || lastEl.getAttribute('data-testid') || lastEl.id || '';
                    }
                    window.__sm_ai_baseline = {
                        count: count,
                        lastText: lastText,
                        lastId: lastId,
                        recordedAt: Date.now()
                    };
                    return JSON.stringify(window.__sm_ai_baseline);
                } catch (e) {
                    window.__sm_ai_baseline = { count: 0, lastText: '', lastId: '', recordedAt: Date.now() };
                    return JSON.stringify(window.__sm_ai_baseline);
                }
            })();
        """.trimIndent()
    }

    /** 프롬프트 입력 전 기준선 기록 + 입력창 탐색 및 입력 + 전송 버튼 탐색 및 클릭 시도 */
    fun injectPromptScript(provider: DirectAIProvider, prompt: String): String {
        val encodedPrompt = jsStringLiteral(prompt)
        return """
            (function() {
                try {
                    // 1. 입력 전 기준선(baseline) 기록
                    var selectorGroups = [
                        'div[data-message-author-role="assistant"]',
                        'div.model-response-text, model-response, message-content',
                        'div.font-claude-message, div[data-testid="chat-message-assistant"], div[class*="font-claude"]',
                        'div[class*="assistant"]',
                        'div[data-is-streaming]',
                        '.markdown.prose'
                    ];
                    var assistantEls = [];
                    for (var i = 0; i < selectorGroups.length; i++) {
                        var els = document.querySelectorAll(selectorGroups[i]);
                        if (els && els.length > 0) {
                            assistantEls = els;
                            break;
                        }
                    }
                    var count = assistantEls.length;
                    var lastText = '';
                    var lastId = '';
                    if (count > 0) {
                        var lastEl = assistantEls[count - 1];
                        lastText = (lastEl.innerText || lastEl.textContent || '').trim();
                        lastId = lastEl.getAttribute('data-message-id') || lastEl.getAttribute('data-testid') || lastEl.id || '';
                    }
                    window.__sm_ai_baseline = {
                        count: count,
                        lastText: lastText,
                        lastId: lastId,
                        recordedAt: Date.now()
                    };

                    var promptText = $encodedPrompt;
                    var inputEl = null;

                    // 2. 제공사별 입력 요소 탐색
                    var inputSelectors = [
                        '#prompt-textarea',
                        'div[contenteditable="true"][data-placeholder]',
                        'div[contenteditable="true"]',
                        'rich-textarea p',
                        'textarea[placeholder]',
                        'textarea',
                        'fieldset div[contenteditable="true"]'
                    ];

                    for (var k = 0; k < inputSelectors.length; k++) {
                        var el = document.querySelector(inputSelectors[k]);
                        if (el && (el.offsetWidth > 0 || el.offsetHeight > 0)) {
                            inputEl = el;
                            break;
                        }
                    }

                    if (!inputEl) {
                        return JSON.stringify({
                            success: false,
                            inputFound: false,
                            submitted: false,
                            error: 'INPUT_NOT_FOUND'
                        });
                    }

                    // 3. 값 설정 및 이벤트 디스패치
                    inputEl.focus();
                    if (inputEl.tagName.toLowerCase() === 'textarea' || inputEl.tagName.toLowerCase() === 'input') {
                        inputEl.value = promptText;
                        inputEl.dispatchEvent(new Event('input', { bubbles: true }));
                        inputEl.dispatchEvent(new Event('change', { bubbles: true }));
                    } else {
                        // contenteditable (ProseMirror / Lexical / Quill)
                        if (document.execCommand) {
                            document.execCommand('selectAll', false, null);
                            document.execCommand('insertText', false, promptText);
                        } else {
                            inputEl.innerText = promptText;
                        }
                        inputEl.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: promptText }));
                        inputEl.dispatchEvent(new Event('input', { bubbles: true }));
                        inputEl.dispatchEvent(new Event('change', { bubbles: true }));
                    }

                    // 4. 전송 버튼 탐색 및 클릭 시도
                    var submitSelectors = [
                        'button[data-testid="send-button"]',
                        'button[data-testid="fruitjuice-send-button"]',
                        'button[aria-label*="전송"]',
                        'button[aria-label*="보내기"]',
                        'button[aria-label*="Send"]',
                        'button[aria-label*="send"]',
                        'button[aria-label*="Send message"]',
                        'button[aria-label*="Send Message"]',
                        'button[aria-label*="Send prompt"]',
                        'button[type="submit"]',
                        '.send-button',
                        'button.send-button'
                    ];
                    var sendBtn = null;
                    for (var j = 0; j < submitSelectors.length; j++) {
                        var btn = document.querySelector(submitSelectors[j]);
                        if (btn && (btn.offsetWidth > 0 || btn.offsetHeight > 0)) {
                            sendBtn = btn;
                            break;
                        }
                    }

                    var submitted = false;
                    if (sendBtn && !sendBtn.disabled && sendBtn.getAttribute('aria-disabled') !== 'true') {
                        sendBtn.click();
                        submitted = true;
                    }

                    return JSON.stringify({
                        success: true,
                        inputFound: true,
                        submitted: submitted,
                        error: null
                    });
                } catch (e) {
                    return JSON.stringify({
                        success: false,
                        inputFound: false,
                        submitted: false,
                        error: e.message || String(e)
                    });
                }
            })();
        """.trimIndent()
    }

    /** 기준선(baseline) 대비 신규 답변 여부, 생성 진행 여부, 추출 텍스트를 구조화해 반환 */
    fun extractAnswerScript(provider: DirectAIProvider): String {
        return """
            (function() {
                try {
                    var baseline = window.__sm_ai_baseline;
                    if (!baseline) {
                        return JSON.stringify({
                            newAnswer: false,
                            generating: false,
                            text: ''
                        });
                    }

                    // 1. 생성/스트리밍/중지 인디케이터 탐색
                    var isGenerating = false;
                    var stopSelectors = [
                        'button[data-testid="stop-button"]',
                        'button[aria-label*="Stop"]',
                        'button[aria-label*="stop"]',
                        'button[aria-label*="중지"]',
                        'button[aria-label*="생성 중단"]',
                        'button[aria-label*="답변 중단"]',
                        'button[mattooltip*="중지"]',
                        'div[data-is-streaming="true"]',
                        'div.result-streaming',
                        '.result-streaming',
                        '.streaming',
                        'span.streaming-cursor',
                        'mat-progress-bar',
                        'div.sparkle-loading',
                        'div.loading-indicator'
                    ];
                    for (var s = 0; s < stopSelectors.length; s++) {
                        var stopEl = document.querySelector(stopSelectors[s]);
                        if (stopEl && (stopEl.offsetWidth > 0 || stopEl.offsetHeight > 0)) {
                            isGenerating = true;
                            break;
                        }
                    }

                    // 2. 어시스턴트 응답 요소 탐색
                    var selectorGroups = [
                        'div[data-message-author-role="assistant"]',
                        'div.model-response-text, model-response, message-content',
                        'div.font-claude-message, div[data-testid="chat-message-assistant"], div[class*="font-claude"]',
                        'div[class*="assistant"]',
                        'div[data-is-streaming]',
                        '.markdown.prose'
                    ];
                    var assistantEls = [];
                    for (var i = 0; i < selectorGroups.length; i++) {
                        var els = document.querySelectorAll(selectorGroups[i]);
                        if (els && els.length > 0) {
                            assistantEls = els;
                            break;
                        }
                    }

                    var currentCount = assistantEls.length;
                    if (currentCount === 0) {
                        return JSON.stringify({
                            newAnswer: false,
                            generating: isGenerating,
                            text: ''
                        });
                    }

                    var lastEl = assistantEls[currentCount - 1];
                    var currentText = (lastEl.innerText || lastEl.textContent || '').trim();
                    var currentId = lastEl.getAttribute('data-message-id') || lastEl.getAttribute('data-testid') || lastEl.id || '';

                    // 3. 기준선 대비 신규 응답 여부 검증
                    var isNew = false;
                    if (currentCount > baseline.count) {
                        isNew = true;
                    } else if (currentCount === baseline.count && baseline.count > 0) {
                        if (currentId && baseline.lastId && currentId !== baseline.lastId) {
                            isNew = true;
                        }
                    }

                    if (!isNew || (currentText === baseline.lastText && baseline.lastText.length > 0)) {
                        return JSON.stringify({
                            newAnswer: false,
                            generating: isGenerating,
                            text: ''
                        });
                    }

                    return JSON.stringify({
                        newAnswer: true,
                        generating: isGenerating,
                        text: currentText
                    });
                } catch (e) {
                    return JSON.stringify({
                        newAnswer: false,
                        generating: false,
                        text: ''
                    });
                }
            })();
        """.trimIndent()
    }

    /** evaluateJavascript의 반환값을 안전하게 파싱 */
    fun parseInjectionResult(rawResult: String?): ExternalAIInjectionResult {
        if (rawResult == null || rawResult == "null" || rawResult.isBlank()) {
            return ExternalAIInjectionResult(
                success = false,
                inputFound = false,
                submitted = false,
                error = "NO_RESPONSE"
            )
        }
        return try {
            val element = parseJsonElement(rawResult)
                ?: return ExternalAIInjectionResult(
                    success = false,
                    inputFound = false,
                    submitted = false,
                    error = "INVALID_RESPONSE"
                )

            when {
                element.isJsonObject -> {
                    val jsonObject = element.asJsonObject
                    ExternalAIInjectionResult(
                        success = jsonObject.optBoolean("success", false),
                        inputFound = jsonObject.optBoolean("inputFound", false),
                        submitted = jsonObject.optBoolean("submitted", false),
                        error = jsonObject.optNullableString("error")
                    )
                }
                element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                    when (val text = element.asString) {
                        "INPUT_SUCCESS" -> ExternalAIInjectionResult(
                            success = true,
                            inputFound = true,
                            submitted = true
                        )
                        "INPUT_NOT_FOUND" -> ExternalAIInjectionResult(
                            success = false,
                            inputFound = false,
                            submitted = false,
                            error = "INPUT_NOT_FOUND"
                        )
                        else -> ExternalAIInjectionResult(
                            success = false,
                            inputFound = false,
                            submitted = false,
                            error = text.ifBlank { null }
                        )
                    }
                }
                else -> ExternalAIInjectionResult(
                    success = false,
                    inputFound = false,
                    submitted = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            ExternalAIInjectionResult(
                success = false,
                inputFound = false,
                submitted = false,
                error = e.message
            )
        }
    }

    /** 제공사 화면에 표시된 오류 요소(에러 배너, 경고 메시지 등)를 감지하는 스크립트 */
    fun extractErrorScript(): String {
        return """
            (function() {
                try {
                    var errorSelectors = [
                        '[data-testid="error-message"]',
                        '[data-testid="error-banner"]',
                        'div[role="alert"]',
                        'div.error-message',
                        'div.text-red-500',
                        'p.text-red-500',
                        'div[class*="error-message"]',
                        'div[class*="ErrorMessage"]',
                        'div[class*="danger"]',
                        '.alert-danger',
                        'div.snack-bar'
                    ];
                    for (var i = 0; i < errorSelectors.length; i++) {
                        var el = document.querySelector(errorSelectors[i]);
                        if (el && (el.offsetWidth > 0 || el.offsetHeight > 0)) {
                            var text = (el.innerText || el.textContent || '').trim();
                            if (text.length > 0) {
                                return JSON.stringify({ hasError: true, error: text });
                            }
                        }
                    }
                    return JSON.stringify({ hasError: false, error: null });
                } catch (e) {
                    return JSON.stringify({ hasError: false, error: null });
                }
            })();
        """.trimIndent()
    }

    /** evaluateJavascript의 오류 감지 반환값을 안전하게 파싱 */
    fun parseErrorResult(rawResult: String?): ExternalAIDomErrorResult {
        if (rawResult == null || rawResult == "null" || rawResult.isBlank()) {
            return ExternalAIDomErrorResult(hasError = false, error = null)
        }
        return try {
            val element = parseJsonElement(rawResult)
                ?: return ExternalAIDomErrorResult(hasError = false, error = null)
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                ExternalAIDomErrorResult(
                    hasError = obj.optBoolean("hasError", false),
                    error = obj.optNullableString("error")
                )
            } else {
                ExternalAIDomErrorResult(hasError = false, error = null)
            }
        } catch (_: Exception) {
            ExternalAIDomErrorResult(hasError = false, error = null)
        }
    }

    /** 로그인 또는 보안 캡차 챌린지 감지 스크립트 */
    fun checkChallengeScript(): String {
        return """
            (function() {
                try {
                    var challengeSelectors = [
                        '#cf-challenge-running',
                        '#challenge-stage',
                        'div.cf-turnstile',
                        'iframe[src*="cloudflare"]',
                        'iframe[src*="recaptcha"]',
                        'div.g-recaptcha',
                        'a[href*="/login"]',
                        'button[data-testid="login-button"]'
                    ];
                    for (var i = 0; i < challengeSelectors.length; i++) {
                        var el = document.querySelector(challengeSelectors[i]);
                        if (el && (el.offsetWidth > 0 || el.offsetHeight > 0)) {
                            return JSON.stringify({ hasChallenge: true });
                        }
                    }
                    return JSON.stringify({ hasChallenge: false });
                } catch (e) {
                    return JSON.stringify({ hasChallenge: false });
                }
            })();
        """.trimIndent()
    }

    /** evaluateJavascript의 챌린지 감지 반환값을 파싱 */
    fun parseChallengeResult(rawResult: String?): Boolean {
        if (rawResult == null || rawResult == "null" || rawResult.isBlank()) {
            return false
        }
        return try {
            val element = parseJsonElement(rawResult) ?: return false
            if (element.isJsonObject) {
                element.asJsonObject.optBoolean("hasChallenge", false)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /** evaluateJavascript의 폴링 반환값을 안전하게 파싱 */
    fun parsePollResult(rawResult: String?): ExternalAIPollResult {
        if (rawResult == null || rawResult == "null" || rawResult.isBlank()) {
            return ExternalAIPollResult(newAnswer = false, generating = false, text = "")
        }
        return try {
            val element = parseJsonElement(rawResult)
                ?: return ExternalAIPollResult(newAnswer = false, generating = false, text = "")

            if (element.isJsonObject) {
                val jsonObject = element.asJsonObject
                ExternalAIPollResult(
                    newAnswer = jsonObject.optBoolean("newAnswer", false),
                    generating = jsonObject.optBoolean("generating", false),
                    text = jsonObject.optString("text", "")
                )
            } else {
                ExternalAIPollResult(newAnswer = false, generating = false, text = "")
            }
        } catch (e: Exception) {
            ExternalAIPollResult(newAnswer = false, generating = false, text = "")
        }
    }

    private fun parseJsonElement(rawResult: String): JsonElement? {
        val trimmed = rawResult.trim()
        if (trimmed.isEmpty() || trimmed == "null") {
            return null
        }
        return try {
            val parsed = JsonParser.parseString(trimmed)
            if (parsed.isJsonNull) {
                null
            } else if (parsed.isJsonPrimitive && parsed.asJsonPrimitive.isString) {
                val innerString = parsed.asString.trim()
                if (innerString.isEmpty() || innerString == "null") {
                    parsed
                } else if (innerString.startsWith("{") || innerString.startsWith("[")) {
                    try {
                        val innerParsed = JsonParser.parseString(innerString)
                        if (innerParsed.isJsonObject || innerParsed.isJsonArray) {
                            innerParsed
                        } else {
                            parsed
                        }
                    } catch (_: Exception) {
                        parsed
                    }
                } else {
                    parsed
                }
            } else {
                parsed
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.optBoolean(key: String, defaultValue: Boolean): Boolean {
        val element = get(key) ?: return defaultValue
        if (element.isJsonNull) return defaultValue
        return try {
            if (element.isJsonPrimitive) {
                val prim = element.asJsonPrimitive
                if (prim.isBoolean) {
                    prim.asBoolean
                } else if (prim.isString) {
                    prim.asString.toBooleanStrictOrNull() ?: defaultValue
                } else {
                    defaultValue
                }
            } else {
                defaultValue
            }
        } catch (_: Exception) {
            defaultValue
        }
    }

    private fun JsonObject.optString(key: String, defaultValue: String = ""): String {
        val element = get(key) ?: return defaultValue
        if (element.isJsonNull) return defaultValue
        return try {
            if (element.isJsonPrimitive) {
                val prim = element.asJsonPrimitive
                if (prim.isString) {
                    prim.asString
                } else {
                    prim.toString()
                }
            } else {
                element.toString()
            }
        } catch (_: Exception) {
            defaultValue
        }
    }

    private fun JsonObject.optNullableString(key: String): String? {
        val element = get(key) ?: return null
        if (element.isJsonNull) return null
        return try {
            if (element.isJsonPrimitive) {
                val prim = element.asJsonPrimitive
                if (prim.isString) {
                    prim.asString
                } else {
                    prim.toString()
                }
            } else {
                element.toString()
            }
        } catch (_: Exception) {
            null
        }
    }
}
