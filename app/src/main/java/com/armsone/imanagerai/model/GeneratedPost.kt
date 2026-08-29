package com.armsone.imanagerai.model

import com.armsone.imanagerai.text.Graphemes
import java.util.UUID

/**
 * 생성된 게시물 초안 한 건. iOS GeneratedPost.swift 포팅.
 * 글자 수는 모두 Swift Character(확장 문자소 클러스터) 단위로 센다.
 */
data class GeneratedPost(
    val sourceIdea: String,
    val hook: String,
    val caption: String,
    val callToAction: String,
    val hashtags: List<String>,
    val composedText: String,
    val targetCharacterCount: Int? = null,
    val destinationCharacterLimit: Int? = null,
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun create(
            sourceIdea: String,
            hook: String,
            caption: String,
            callToAction: String,
            hashtags: List<String>,
            composedText: String? = null,
            targetCharacterCount: Int? = null,
            destinationCharacterLimit: Int? = null,
            id: String = UUID.randomUUID().toString(),
            createdAt: Long = System.currentTimeMillis()
        ): GeneratedPost = GeneratedPost(
            sourceIdea = sourceIdea,
            hook = hook,
            caption = caption,
            callToAction = callToAction,
            hashtags = hashtags,
            composedText = composedText ?: assembleText(hook, caption, callToAction, hashtags),
            targetCharacterCount = targetCharacterCount,
            destinationCharacterLimit = destinationCharacterLimit,
            id = id,
            createdAt = createdAt
        )

        private fun assembleText(
            hook: String,
            caption: String,
            callToAction: String,
            hashtags: List<String>
        ): String {
            val lines = mutableListOf<String>()
            if (hashtags.isNotEmpty()) {
                lines.add(hashtags.joinToString(" ") { "#$it" })
            }
            for (part in listOf(hook, caption, callToAction)) {
                if (part.isNotEmpty()) lines.add(part)
            }
            return lines.joinToString("\n")
        }
    }

    /** 목록형 UI에 쓰기 좋은 한 줄 제목. */
    val listTitle: String
        get() {
            val source = sourceIdea.ifEmpty { hook }
            val firstLine = source.split("\n", limit = 2).firstOrNull() ?: source
            return if (Graphemes.count(firstLine) > 24) Graphemes.prefix(firstLine, 24) + "…" else firstLine
        }

    /** 목록형 UI의 부제로 쓰기 좋은 본문 요약. */
    val previewSnippet: String
        get() {
            val body = composedText.replace("\n", " ")
            return if (Graphemes.count(body) > 60) Graphemes.prefix(body, 60) + "…" else body
        }

    /** 공백/줄바꿈 포함 Character 단위 글자 수. */
    val characterCount: Int get() = Graphemes.count(composedText)

    val formatReport: CaptionFormatReport
        get() = CaptionFormatReport.evaluate(composedText, destinationCharacterLimit ?: CaptionFormatReport.NEUTRAL_SAFETY_LIMIT)
}

/** 게시 기준 글자 수 한도 준수 검증 결과. iOS CaptionFormatReport 포팅. */
data class CaptionFormatReport(
    val destinationLimit: Int,
    val characterCount: Int,
    val isWithinDestinationLimit: Boolean
) {
    val passesAllRules: Boolean get() = isWithinDestinationLimit

    val failedRuleDescriptions: List<String>
        get() = if (isWithinDestinationLimit) emptyList() else listOf("게시 기준 ${destinationLimit}자 이내 (현재 ${characterCount}자)")

    // 레거시 테스트 및 호환 프로퍼티
    val requiredCharacterCount: Int get() = destinationLimit

    companion object {
        const val NEUTRAL_SAFETY_LIMIT = 20_000

        fun evaluate(text: String, destinationLimit: Int = NEUTRAL_SAFETY_LIMIT): CaptionFormatReport {
            val count = Graphemes.count(text)
            return CaptionFormatReport(
                destinationLimit = destinationLimit,
                characterCount = count,
                isWithinDestinationLimit = count <= destinationLimit
            )
        }
    }
}

/** 외부 AI에서 돌아온 원문을 개인 설정까지 포함해 검사하기 위한 문맥. */
data class CaptionValidationContext(
    val destinationLimit: Int,
    val prohibitedPhrases: String = "",
    val emojiIntensity: EmojiIntensity
)

/** 기본 형식 검사 + 이모지 안 씀/글자 수 표기 검사 (금지 표현은 비활성화). iOS CaptionValidationReport 포팅. */
data class CaptionValidationReport(
    val format: CaptionFormatReport,
    val prohibitedPhraseMatches: List<String> = emptyList(),
    val respectsEmojiNonePreference: Boolean,
    val hasNoCharacterCountLabel: Boolean
) {
    val passesAllRules: Boolean
        get() = format.passesAllRules &&
            prohibitedPhraseMatches.isEmpty() &&
            respectsEmojiNonePreference &&
            hasNoCharacterCountLabel

    val failedRuleDescriptions: List<String>
        get() = buildList {
            addAll(format.failedRuleDescriptions)
            if (prohibitedPhraseMatches.isNotEmpty()) {
                add("금지 표현 제외: ${prohibitedPhraseMatches.joinToString(", ")}")
            }
            if (!respectsEmojiNonePreference) add("이모지 안 씀 설정 준수")
            if (!hasNoCharacterCountLabel) add("글자 수 표기 금지")
        }

    companion object {
        private val COUNT_LABEL_REGEX = Regex("글자\\s*수|[0-9]+\\s*자")

        fun evaluate(text: String, context: CaptionValidationContext): CaptionValidationReport {
            val containsEmoji = Graphemes.containsEmoji(text)
            val hasCountLabel = COUNT_LABEL_REGEX.containsMatchIn(text)

            return CaptionValidationReport(
                format = CaptionFormatReport.evaluate(text, context.destinationLimit),
                prohibitedPhraseMatches = emptyList(),
                respectsEmojiNonePreference = context.emojiIntensity != EmojiIntensity.NONE || !containsEmoji,
                hasNoCharacterCountLabel = !hasCountLabel
            )
        }
    }
}
