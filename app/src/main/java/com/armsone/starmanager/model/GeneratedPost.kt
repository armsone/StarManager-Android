package com.armsone.starmanager.model

import com.armsone.starmanager.text.Graphemes
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
    val targetCharacterCount: Int? = 200,
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
            targetCharacterCount: Int = 200,
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
        get() = CaptionFormatReport.evaluate(composedText, targetCharacterCount ?: 200)
}

/** 저장된 작성 지침의 필수 형식 규칙 검증 결과. iOS CaptionFormatReport 포팅. */
data class CaptionFormatReport(
    val requiredCharacterCount: Int,
    val characterCount: Int,
    val hasExactCharacterCount: Boolean,
    val firstLineHasTwoKoreanHashtags: Boolean,
    val periodsAlwaysEndLines: Boolean,
    val hasNoFullTextQuotes: Boolean,
    val emojiUsageIsRestrained: Boolean,
    val lastLineIsEmojiWrappedSummary: Boolean
) {
    val passesAllRules: Boolean
        get() = hasExactCharacterCount &&
            firstLineHasTwoKoreanHashtags &&
            periodsAlwaysEndLines &&
            hasNoFullTextQuotes &&
            emojiUsageIsRestrained &&
            lastLineIsEmojiWrappedSummary

    val failedRuleDescriptions: List<String>
        get() = buildList {
            if (!hasExactCharacterCount) {
                add("공백 포함 ${requiredCharacterCount}자 (현재 ${characterCount}자)")
            }
            if (!firstLineHasTwoKoreanHashtags) add("첫 줄 한글 해시태그 2개 연속")
            if (!periodsAlwaysEndLines) add("마침표 뒤 줄바꿈")
            if (!hasNoFullTextQuotes) add("전체 따옴표 금지")
            if (!emojiUsageIsRestrained) add("이모지 절제 사용")
            if (!lastLineIsEmojiWrappedSummary) add("마지막 줄 이모지로 감싼 요약")
        }

    companion object {
        private val QUOTE_MARKS = listOf("\"", "“", "”")

        fun evaluate(text: String, requiredCharacterCount: Int = 200): CaptionFormatReport {
            val lines = text.split("\n")
            val trimmed = text.trim()
            val trimmedClusters = Graphemes.clusters(trimmed)

            val wrappedInQuotes = QUOTE_MARKS.any { mark ->
                trimmedClusters.size >= 2 && trimmed.startsWith(mark) &&
                    QUOTE_MARKS.contains(trimmedClusters.last())
            }

            return CaptionFormatReport(
                requiredCharacterCount = requiredCharacterCount,
                characterCount = Graphemes.count(text),
                hasExactCharacterCount = Graphemes.count(text) == requiredCharacterCount,
                firstLineHasTwoKoreanHashtags = firstLineHasHashtagPair(lines.firstOrNull() ?: ""),
                periodsAlwaysEndLines = lines.all { line ->
                    val clusters = Graphemes.clusters(line)
                    clusters.dropLast(1).none { it == "." }
                },
                hasNoFullTextQuotes = !wrappedInQuotes,
                emojiUsageIsRestrained = emojiIsRestrained(lines),
                lastLineIsEmojiWrappedSummary = lastLineIsWrappedSummary(lines.lastOrNull() ?: "")
            )
        }

        private fun firstLineHasHashtagPair(line: String): Boolean {
            val tokens = line.split(" ").filter { it.isNotEmpty() }
            if (tokens.size != 2) return false
            return tokens.all { token ->
                val clusters = Graphemes.clusters(token)
                clusters.size >= 2 && token.startsWith("#") &&
                    clusters.drop(1).all { Graphemes.isHangulCluster(it) }
            }
        }

        private fun lastLineIsWrappedSummary(line: String): Boolean {
            val clusters = Graphemes.clusters(line)
            if (clusters.size < 3) return false
            val first = clusters.first()
            val last = clusters.last()
            val core = clusters.subList(1, clusters.size - 1).joinToString("").trim(' ', '\t')
            return Graphemes.isEmojiCluster(first) && Graphemes.isEmojiCluster(last) && core.isNotEmpty()
        }

        /** 첫 줄에는 이모지가 없어야 하고, 본문 줄은 문단 앞에만, 마지막 줄은 앞뒤 배치만 허용. */
        private fun emojiIsRestrained(lines: List<String>): Boolean {
            lines.forEachIndexed { index, line ->
                val clusters = Graphemes.clusters(line)
                clusters.forEachIndexed { position, cluster ->
                    if (Graphemes.isEmojiCluster(cluster)) {
                        if (index == 0) return false
                        val isLastLine = index == lines.size - 1
                        val allowed = position == 0 || (isLastLine && position == clusters.size - 1)
                        if (!allowed) return false
                    }
                }
            }
            return true
        }
    }
}

/** 외부 AI에서 돌아온 원문을 개인 설정까지 포함해 검사하기 위한 문맥. */
data class CaptionValidationContext(
    val requiredCharacterCount: Int,
    val prohibitedPhrases: String,
    val allowsBodyEmoji: Boolean,
    val minimumLineCount: Int = 3
)

/** 기본 형식 검사 + 금지 표현/본문 이모지/글자 수 표기/최소 문단 검사. */
data class CaptionValidationReport(
    val format: CaptionFormatReport,
    val prohibitedPhraseMatches: List<String>,
    val respectsBodyEmojiPreference: Boolean,
    val hasNoCharacterCountLabel: Boolean,
    val hasMinimumLineCount: Boolean
) {
    val passesAllRules: Boolean
        get() = format.passesAllRules &&
            prohibitedPhraseMatches.isEmpty() &&
            respectsBodyEmojiPreference &&
            hasNoCharacterCountLabel &&
            hasMinimumLineCount

    val failedRuleDescriptions: List<String>
        get() = buildList {
            addAll(format.failedRuleDescriptions)
            if (prohibitedPhraseMatches.isNotEmpty()) {
                add("금지 표현 제외: ${prohibitedPhraseMatches.joinToString(", ")}")
            }
            if (!respectsBodyEmojiPreference) add("본문 이모지 설정 준수")
            if (!hasNoCharacterCountLabel) add("글자 수 표기 금지")
            if (!hasMinimumLineCount) add("태그·본문·요약 문단 구성")
        }

    companion object {
        private val COUNT_LABEL_REGEX = Regex("글자\\s*수|[0-9]+\\s*자")

        fun evaluate(text: String, context: CaptionValidationContext): CaptionValidationReport {
            val lines = text.split("\n")
            val prohibited = context.prohibitedPhrases
                .split(Regex("[,;\n]"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val matches = prohibited.filter { text.contains(it, ignoreCase = true) }
            val bodyLines = lines.drop(1).dropLast(1)
            val bodyContainsEmoji = Graphemes.containsEmoji(bodyLines.joinToString("\n"))
            val hasCountLabel = COUNT_LABEL_REGEX.containsMatchIn(text)

            return CaptionValidationReport(
                format = CaptionFormatReport.evaluate(text, context.requiredCharacterCount),
                prohibitedPhraseMatches = matches,
                respectsBodyEmojiPreference = context.allowsBodyEmoji || !bodyContainsEmoji,
                hasNoCharacterCountLabel = !hasCountLabel,
                hasMinimumLineCount = lines.size >= context.minimumLineCount
            )
        }
    }
}
