package com.armsone.starmanager.service

import com.armsone.starmanager.model.CaptionValidationContext
import com.armsone.starmanager.model.CaptionValidationReport
import com.armsone.starmanager.model.CreatorProfile
import com.armsone.starmanager.model.GeneratedPost
import com.armsone.starmanager.model.GenerationControls
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.text.Graphemes
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
 * Android에는 Apple FoundationModels가 없으므로 iOS의 미지원 기기와 동일하게
 * 항상 UNSUPPORTED_DEVICE("기본 생성")로 동작한다.
 */
enum class DeviceIntelligenceAvailability {
    AVAILABLE,
    UNSUPPORTED_DEVICE,
    NEEDS_APPLE_INTELLIGENCE,
    MODEL_DOWNLOADING;

    val title: String
        get() = when (this) {
            AVAILABLE -> "아이폰 AI"
            else -> "기본 생성"
        }

    val detail: String
        get() = when (this) {
            AVAILABLE -> "로그인 없이 기기에서 작성"
            UNSUPPORTED_DEVICE -> "이 기기에서도 바로 작성"
            NEEDS_APPLE_INTELLIGENCE -> "Apple Intelligence를 켜면 기기 AI 사용"
            MODEL_DOWNLOADING -> "기기 AI 준비가 끝날 때까지 기본 생성"
        }

    companion object {
        val current: DeviceIntelligenceAvailability = UNSUPPORTED_DEVICE
    }
}

/** 외부 AI 제공사 — 프롬프트 공유/붙여넣기 흐름에서 사용. */
enum class DirectAIProvider(val rawValue: String) {
    OPEN_AI("openAI"),
    GEMINI("gemini"),
    GROK("grok");

    val title: String
        get() = when (this) {
            OPEN_AI -> "ChatGPT"
            GEMINI -> "Gemini"
            GROK -> "Grok"
        }
}

/** iOS ComposerView의 외부 프롬프트 조립을 공용화한 것. */
object ExternalPromptBuilder {
    fun build(profile: CreatorProfile, idea: String, mood: PostMood, length: PostLength): String =
        profile.prompt(idea) + "\n" +
            "- 선택한 분위기: ${mood.rawValue}\n" +
            "- 이야기 비중: ${length.storyWeightTitle} — ${length.promptInstruction}\n" +
            "- 공백과 줄바꿈을 포함해 정확히 ${profile.controls.characterCount}자로 작성"

    fun validationContext(profile: CreatorProfile): CaptionValidationContext =
        CaptionValidationContext(
            requiredCharacterCount = profile.controls.characterCount,
            prohibitedPhrases = profile.prohibitedPhrases,
            allowsBodyEmoji = profile.usesEmoji
        )

    fun validate(text: String, profile: CreatorProfile): CaptionValidationReport =
        CaptionValidationReport.evaluate(text, validationContext(profile))
}

/**
 * iOS PreviewCaptionGenerator 포팅 — 오프라인 결정적 생성기.
 * 저장된 작성 지침의 필수 형식(정확한 글자 수, 첫 줄 한글 태그 2개,
 * 마침표 뒤 줄바꿈, 전체 따옴표 금지, 절제된 문단 앞 이모지,
 * 이모지로 감싼 요약 마지막 줄)을 결정적으로 재현한다.
 * 시드/난수는 Swift UInt64 오버플로 연산(&*, &+, >>)을 ULong으로 그대로 따른다.
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
        val controls = profile.controls
        val seed = SeedBox(seed(cleanIdea + mood.rawValue + length.rawValue + controls.swiftDescription()))

        val tags = hashtagPair(cleanIdea, mood, seed)
        val firstLine = "#${tags.first} #${tags.second}"
        val lastLine = summaryLine(mood, controls.characterCount)

        if (controls.characterCount < 100) {
            val bodyLength = maxOf(
                1,
                controls.characterCount - gc(firstLine) - gc(lastLine) - 2
            )
            val body = compactBody(bodyLength, cleanIdea, mood, length, profile.usesEmoji)
            val composedText = listOf(firstLine, body, lastLine).joinToString("\n")
            return GeneratedPost.create(
                sourceIdea = cleanIdea,
                hook = body,
                caption = "",
                callToAction = lastLine,
                hashtags = listOf(tags.first, tags.second),
                composedText = composedText,
                targetCharacterCount = controls.characterCount
            )
        }

        val maximumLeadLength = maxOf(
            4,
            controls.characterCount - gc(firstLine) - gc(lastLine) - 4
        )
        val leadLine = leadLine(
            idea = cleanIdea,
            mood = mood,
            cap = minOf(leadCap(length), maximumLeadLength),
            emoji = if (profile.usesEmoji) paragraphEmoji(mood) else null
        )

        // 전체 글자 수 = 각 줄 글자 수 합 + 줄바꿈 수(줄 수 - 1).
        // 고정 줄을 뺀 나머지 예산을 본문 문장으로 채우고,
        // 마지막 남은 글자 수는 가변 길이 문장으로 정확히 메운다.
        var remaining = controls.characterCount -
            gc(firstLine) -
            (1 + gc(leadLine)) -
            (1 + gc(lastLine))

        val bodyLines = mutableListOf(leadLine)
        val banned = bannedPhrases(profile.prohibitedPhrases)
        val bank = toneSentenceBank(controls) +
            rotated(sentenceBank(mood), seed).filter { sentence -> banned.none { sentence.contains(it) } }

        for (sentence in bank) {
            val cost = 1 + gc(sentence)
            // 가변 문장 최소 비용(줄바꿈 1 + 글자 3)을 항상 남겨 둔다.
            if (remaining - cost >= 4) {
                bodyLines.add(sentence)
                remaining -= cost
            }
        }
        bodyLines.add(flexSentence(remaining - 1, seed))

        val composedText = (listOf(firstLine) + bodyLines + listOf(lastLine)).joinToString("\n")

        return GeneratedPost.create(
            sourceIdea = cleanIdea,
            hook = leadLine,
            caption = bodyLines.drop(1).joinToString("\n"),
            callToAction = lastLine,
            hashtags = listOf(tags.first, tags.second),
            composedText = composedText,
            targetCharacterCount = controls.characterCount
        )
    }

    companion object {

        private fun gc(text: String): Int = Graphemes.count(text)

        // MARK: - 입력 정리

        /** 형식 규칙과 충돌하는 문자(따옴표, 해시, 이모지, 문장 중간 마침표)를 제거한다. */
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

        private fun leadCap(length: PostLength): Int = when (length) {
            PostLength.SHORT -> 45
            PostLength.MEDIUM -> 65
            PostLength.LONG -> 90
        }

        private fun leadLine(idea: String, mood: PostMood, cap: Int, emoji: String?): String {
            val prefix = emoji ?: ""
            val clause = idea.ifEmpty { defaultLead(mood) }
            val bodyCap = cap - gc(prefix) - 1

            if (gc(clause) <= bodyCap) {
                return "$prefix$clause."
            }
            // 해시태그·이모지·요약을 건드리지 않도록 아이디어 문장만 단어 경계에서 줄인다.
            var cut = Graphemes.prefix(clause, bodyCap)
            val cutClusters = Graphemes.clusters(cut)
            val lastSpace = cutClusters.lastIndexOf(" ")
            if (lastSpace >= 0 && lastSpace > bodyCap / 2) {
                cut = cutClusters.subList(0, lastSpace).joinToString("")
            }
            return prefix + cut.trim(' ', '\t') + "…"
        }

        private fun summaryLine(mood: PostMood, targetCount: Int): String {
            if (targetCount < 100) {
                return when (mood) {
                    PostMood.WARM -> "🧡 오늘을 남긴다 🧡"
                    PostMood.WITTY -> "😎 오늘도 해냈다 😎"
                    PostMood.CALM -> "🌙 오늘을 담는다 🌙"
                }
            }
            val (emoji, clause) = when (mood) {
                PostMood.WARM -> "🧡" to "온기를 담아 오늘을 남긴다"
                PostMood.WITTY -> "😎" to "얼렁뚱땅 그래도 완벽한 하루"
                PostMood.CALM -> "🌙" to "고요하게 채운 하루의 기록"
            }
            return "$emoji $clause $emoji"
        }

        private fun compactBody(
            target: Int,
            idea: String,
            mood: PostMood,
            length: PostLength,
            emoji: Boolean
        ): String {
            if (target <= 0) return ""
            val prefix = if (emoji) paragraphEmoji(mood) else ""
            val ratio = when (length) {
                PostLength.SHORT -> 0.35
                PostLength.MEDIUM -> 0.6
                PostLength.LONG -> 0.85
            }
            val available = maxOf(0, target - gc(prefix) - 1)
            val sourceCount = minOf(gc(idea), maxOf(1, (available * ratio).toInt()))
            var body = prefix + Graphemes.prefix(idea, sourceCount).trim(' ', '\t')

            if (gc(body) >= target) {
                return Graphemes.prefix(body, maxOf(0, target - 1)) + "."
            }

            val seedBox = SeedBox(seed(idea + mood.rawValue + length.rawValue))
            val remaining = target - gc(body)
            if (remaining == 1) return "$body…"
            body += " " + flexSentence(remaining - 1, seedBox)
            if (gc(body) < target) body += "음".repeat(target - gc(body))
            return Graphemes.prefix(body, target)
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

        private fun toneSentenceBank(controls: GenerationControls): List<String> {
            val groups: List<Pair<Int, List<String>>> = listOf(
                controls.emotion to listOf("마음의 잔향이 오래 머문다.", "무심한 장면이 가슴을 건드린다."),
                controls.kindness to listOf("다정한 시선 하나를 조용히 건넨다.", "서두르지 않아도 괜찮다고 말해 본다."),
                controls.originality to listOf("익숙한 풍경의 이면이 낯설게 반짝인다.", "평범함의 모서리에서 새 장면을 줍는다."),
                controls.masculinity to listOf("결심한 방향으로 묵묵히 걸어간다.", "말보다 단단한 걸음으로 답한다."),
                controls.chic to listOf("설명은 줄이고 여운만 남긴다.", "담백하게 선을 긋고 다음으로 간다.")
            )
            return groups.sortedByDescending { it.first }.flatMap { it.second }
        }

        // MARK: - 해시태그

        private fun hashtagPair(
            idea: String,
            mood: PostMood,
            seed: SeedBox
        ): Pair<String, String> {
            val candidates = mutableListOf<String>()
            for (word in idea.split(" ").filter { it.isNotEmpty() }) {
                val hangul = Graphemes.clusters(word)
                    .filter { Graphemes.isHangulCluster(it) }
                    .take(6)
                    .joinToString("")
                if (gc(hangul) >= 2 && hangul !in candidates) {
                    candidates.add(hangul)
                }
            }

            val fallback = when (mood) {
                PostMood.WARM -> listOf("온기기록", "마음한켠", "따뜻한하루")
                PostMood.WITTY -> listOf("일상반전", "오늘의수확", "얼렁뚱땅")
                PostMood.CALM -> listOf("담백일기", "고요한하루", "느린기록")
            }
            val pool = rotated(fallback, seed).toMutableList()
            while (candidates.size < 2) {
                val next = pool.removeAt(0)
                if (next !in candidates) candidates.add(next)
            }
            return candidates[0] to candidates[1]
        }

        // MARK: - 글자 수 맞춤 채움

        /**
         * 마침표를 포함해 정확히 target 글자인 혼잣말 문장을 합성한다.
         * 부사(길이 2~4, 공백 포함 비용 3~5)를 쌓다가 길이 2~6의 마무리 어절로 닫는다.
         */
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

        // MARK: - 결정적 난수 (Swift UInt64 오버플로 재현)

        internal class SeedBox(var value: ULong)

        /** 같은 아이디어·설정이면 항상 같은 결과가 나오도록 FNV-1a 기반 시드를 쓴다. */
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
