package com.armsone.imanagerai.service

import com.armsone.imanagerai.model.CaptionValidationContext
import com.armsone.imanagerai.model.CaptionValidationReport
import com.armsone.imanagerai.model.CreatorProfile
import com.armsone.imanagerai.model.EmojiIntensity
import com.armsone.imanagerai.model.GeneratedPost
import com.armsone.imanagerai.model.PostLength
import com.armsone.imanagerai.model.PostMood
import com.armsone.imanagerai.text.Graphemes
import kotlinx.coroutines.delay

interface CaptionGenerating {
    suspend fun generate(
        idea: String,
        mood: PostMood,
        length: PostLength,
        profile: CreatorProfile
    ): GeneratedPost
}

/**
 * iOS DeviceIntelligenceAvailability 대응.
 * Android에는 Apple FoundationModels가 없으므로 로컬 결정적 생성기로 동작하며
 * UI에는 "기기 AI"로 표기한다.
 */
enum class DeviceIntelligenceAvailability {
    AVAILABLE,
    UNSUPPORTED_DEVICE,
    NEEDS_APPLE_INTELLIGENCE,
    MODEL_DOWNLOADING;

    val title: String
        get() = "기기 AI"

    val detail: String
        get() = when (this) {
            AVAILABLE -> "로그인 없이 기기에서 작성"
            UNSUPPORTED_DEVICE -> "이 기기에서도 바로 작성"
            NEEDS_APPLE_INTELLIGENCE -> "기기 AI 사용"
            MODEL_DOWNLOADING -> "기기 AI 준비가 끝날 때까지 기본 생성"
        }

    companion object {
        val current: DeviceIntelligenceAvailability = UNSUPPORTED_DEVICE
    }
}

/** 외부 AI 제공사 — 프롬프트 웹 생성 및 로그인 관리에서 사용. */
enum class DirectAIProvider(val rawValue: String, val title: String, val url: String) {
    GEMINI("gemini", "Gemini", "https://gemini.google.com/app"),
    OPEN_AI("openAI", "ChatGPT", "https://chatgpt.com/"),
    CLAUDE("claude", "Claude", "https://claude.ai/new"),
    GROK("grok", "Grok", "https://grok.com/"); // 마이그레이션 호환용 (사용자 노출 불가)

    companion object {
        val visibleProviders: List<DirectAIProvider> = listOf(GEMINI, OPEN_AI, CLAUDE)
    }
}

/** iOS ComposerView의 외부 프롬프트 조립을 공용화한 것. */
object ExternalPromptBuilder {
    const val NEUTRAL_SAFETY_LIMIT = 20_000

    fun build(profile: CreatorProfile, idea: String, mood: PostMood, length: PostLength): String =
        profile.generationPrompt(idea, mood, length)

    fun buildPhotoOnly(profile: CreatorProfile, mood: PostMood, length: PostLength): String =
        profile.photoOnlyPrompt(mood, length)

    fun buildPhotoAndText(profile: CreatorProfile, idea: String, mood: PostMood, length: PostLength): String =
        profile.photoAndTextPrompt(idea, mood, length)

    fun validationContext(profile: CreatorProfile): CaptionValidationContext =
        CaptionValidationContext(
            destinationLimit = NEUTRAL_SAFETY_LIMIT,
            prohibitedPhrases = "",
            emojiIntensity = profile.emojiIntensity
        )

    fun validate(text: String, profile: CreatorProfile): CaptionValidationReport =
        CaptionValidationReport.evaluate(text, validationContext(profile))
}

/**
 * iOS PreviewCaptionGenerator 포팅 — 오프라인 결정적 생성기.
 * 분위기·이모지 강도·원문 반영 정도에 맞춰 문장을 결정적으로 조립한다.
 */
class DeterministicCaptionGenerator(
    private val simulatedDelayMillis: Long = 550L
) : CaptionGenerating {

    override suspend fun generate(
        idea: String,
        mood: PostMood,
        length: PostLength,
        profile: CreatorProfile
    ): GeneratedPost {
        if (simulatedDelayMillis > 0) delay(simulatedDelayMillis)

        val cleanIdea = sanitize(idea)
        val seed = SeedBox(seed(cleanIdea + mood.rawValue + length.rawValue + profile.emojiIntensity.name))
        val limit = profile.controls.characterCount.coerceIn(50, 500)
        val symbol = paragraphEmoji(mood)

        val (leadEmoji, bodyEmoji, summaryEmoji) = when (profile.emojiIntensity) {
            EmojiIntensity.NONE -> Triple<String?, String, String>(null, "", "")
            EmojiIntensity.LOW -> Triple<String?, String, String>(null, "", symbol)
            EmojiIntensity.HIGH -> Triple<String?, String, String>(symbol, "", symbol)
            EmojiIntensity.HEAVY -> Triple<String?, String, String>(symbol, symbol, symbol)
        }

        val leadRatio = when (length) {
            PostLength.SHORT -> 0.35
            PostLength.MEDIUM -> 0.6
            PostLength.LONG -> 0.85
        }
        val leadCap = minOf(maxOf(8, (limit * leadRatio).toInt()), limit - 1)
        val leadLine = leadLine(idea = cleanIdea, mood = mood, cap = leadCap, emoji = leadEmoji)

        val lines = mutableListOf(leadLine)
        var used = Graphemes.count(leadLine)

        val bank = rotated(sentenceBank(mood), seed)

        for (sentence in bank) {
            val candidate = if (bodyEmoji.isEmpty()) sentence else "$bodyEmoji $sentence"
            val cost = 1 + Graphemes.count(candidate)
            if (used + cost <= limit) {
                lines.add(candidate)
                used += cost
            }
        }

        if (summaryEmoji.isNotEmpty()) {
            val summary = "$summaryEmoji ${summaryClause(mood)} $summaryEmoji"
            if (used + 1 + Graphemes.count(summary) <= limit) {
                lines.add(summary)
                used += 1 + Graphemes.count(summary)
            }
        }

        var composedText = lines.joinToString("\n")
        if (Graphemes.count(composedText) > limit) {
            composedText = Graphemes.prefix(composedText, limit)
        }

        return GeneratedPost.create(
            sourceIdea = cleanIdea,
            hook = leadLine,
            caption = lines.drop(1).joinToString("\n"),
            callToAction = lines.lastOrNull() ?: "",
            hashtags = emptyList(),
            composedText = composedText,
            targetCharacterCount = limit,
            destinationCharacterLimit = null
        )
    }

    companion object {

        // MARK: - 입력 정리

        internal fun sanitize(raw: String): String {
            val mapped = Graphemes.clusters(raw).joinToString("") { cluster ->
                when {
                    Graphemes.isEmojiCluster(cluster) || cluster in listOf("\"", "“", "”", "#") -> ""
                    cluster in listOf(".", "!", "?", "…") -> " "
                    else -> cluster
                }
            }
            return Graphemes.nfc(
                mapped.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
            )
        }

        private fun bannedPhrases(raw: String): List<String> =
            raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        // MARK: - 줄 구성

        private fun leadLine(idea: String, mood: PostMood, cap: Int, emoji: String?): String {
            val prefix = emoji?.let { "$it " } ?: ""
            val clause = idea.ifEmpty { defaultLead(mood) }
            val bodyCap = maxOf(1, cap - Graphemes.count(prefix) - 1)

            if (Graphemes.count(clause) <= bodyCap) {
                return "$prefix$clause."
            }
            var cut = Graphemes.prefix(clause, bodyCap)
            val cutClusters = Graphemes.clusters(cut)
            val lastSpace = cutClusters.lastIndexOf(" ")
            if (lastSpace >= 0 && lastSpace > bodyCap / 2) {
                cut = cutClusters.subList(0, lastSpace).joinToString("")
            }
            return prefix + cut.trim(' ', '\t') + "…"
        }

        private fun summaryClause(mood: PostMood): String = when (mood) {
            PostMood.WARM -> "온기를 담아 오늘을 남긴다"
            PostMood.WITTY -> "얼렁뚱땅 그래도 완벽한 하루"
            PostMood.CALM -> "고요하게 채운 하루의 기록"
        }

        private fun paragraphEmoji(mood: PostMood): String = when (mood) {
            PostMood.WARM -> "✨"
            PostMood.WITTY -> "🙃"
            PostMood.CALM -> "🌿"
        }

        private fun defaultLead(mood: PostMood): String = when (mood) {
            PostMood.WARM -> "별일 없던 하루에도 남기고 싶은 온기가 있다"
            PostMood.WITTY -> "오늘도 계획에 없던 장면 하나를 주웠다"
            PostMood.CALM -> "지나가는 하루를 조용히 붙잡아 둔다"
        }

        private fun sentenceBank(mood: PostMood): List<String> = when (mood) {
            PostMood.WARM -> listOf(
                "별것 아닌 장면이 자꾸 마음에 남는다.",
                "따뜻한 기운이 하루 끝까지 따라왔다.",
                "이런 날은 오래 쥐고 싶어진다.",
                "누군가에게도 이 온기가 닿았으면 한다.",
                "작은 순간이 나를 다독인다.",
                "고맙다는 말이 입안에 맴돈다.",
                "내일의 나에게 건네는 응원 같았다.",
                "마음 한 켠이 노곤하게 풀린다."
            )
            PostMood.WITTY -> listOf(
                "계획은 없었는데 결과는 만족이다.",
                "이 정도면 오늘의 승자는 나다.",
                "우연이 실력처럼 보이는 날이 있다.",
                "웃음이 새어 나와서 참지 않았다.",
                "평범한 하루에 반전 하나 끼워 넣었다.",
                "다음에도 이런 우연은 환영이다.",
                "괜히 어깨가 으쓱해진다.",
                "누가 보면 준비한 줄 알겠다."
            )
            PostMood.CALM -> listOf(
                "소란하지 않아서 좋은 하루였다.",
                "천천히 걷다 보니 마음도 느려졌다.",
                "말없이 지나가는 시간을 지켜봤다.",
                "덜어낸 자리에 여백이 남는다.",
                "오늘은 이 정도면 충분하다.",
                "생각을 정리하기 좋은 온도였다.",
                "가라앉은 마음이 나쁘지 않다.",
                "하루의 결이 고르게 느껴진다."
            )
        }

        // MARK: - 결정적 난수 (Swift UInt64 오버플로 재현)

        internal fun flexSentence(target: Int, seed: SeedBox): String {
            when {
                target < 1 -> return ""
                target == 1 -> return "…"
                target == 2 -> return "늘."
            }

            val connectors = listOf(
                listOf("문득", "괜히", "다시", "조금", "슬쩍"),
                listOf("천천히", "가만히", "고요히", "기꺼이", "나직이"),
                listOf("새삼스레", "다정하게", "무던하게", "은근하게")
            )
            val enders = listOf("오늘", "이대로", "잔잔하게", "고즈넉하게", "사부작사부작")

            var remaining = target
            val words = mutableListOf<String>()
            while (remaining > 7) {
                val cost = 3 + (nextRandom(seed) % 3uL).toInt()
                val pool = connectors[cost - 3]
                words.add(pool[(nextRandom(seed) % pool.size.toULong()).toInt()])
                remaining -= cost
            }
            words.add(enders[remaining - 3])
            return words.joinToString(" ") + "."
        }

        internal class SeedBox(var value: ULong)

        internal fun seed(text: String): ULong {
            var hash: ULong = 0xcbf29ce484222325uL
            text.codePoints().forEach { cp ->
                hash = hash xor cp.toULong()
                hash *= 0x100000001b3uL
            }
            return if (hash == 0uL) 1uL else hash
        }

        internal fun nextRandom(seed: SeedBox): ULong {
            seed.value = seed.value * 6364136223846793005uL + 1442695040888963407uL
            return seed.value shr 33
        }

        private fun rotated(items: List<String>, seed: SeedBox): List<String> {
            if (items.isEmpty()) return items
            val offset = (nextRandom(seed) % items.size.toULong()).toInt()
            return items.subList(offset, items.size) + items.subList(0, offset)
        }
    }
}
