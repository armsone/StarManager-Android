package com.armsone.starmanager

import com.armsone.starmanager.model.CaptionValidationReport
import com.armsone.starmanager.model.CreatorProfile
import com.armsone.starmanager.model.EmojiIntensity
import com.armsone.starmanager.model.GenerationControls
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.service.DeterministicCaptionGenerator
import com.armsone.starmanager.service.ExternalPromptBuilder
import com.armsone.starmanager.text.Graphemes
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicCaptionGeneratorTest {

    private val generator = DeterministicCaptionGenerator(simulatedDelayMillis = 0)
    private val idea = "오늘 아침 한강에서 달리기를 하고 커피를 마셨다"

    private fun profileWith(count: Int, emojiIntensity: EmojiIntensity = EmojiIntensity.LOW) = CreatorProfile(
        emojiIntensity = emojiIntensity,
        generationControls = GenerationControls(characterCount = count)
    )

    @Test
    fun `기본 200자 목표를 넘지 않고 실제 문자소를 기록한다`() = runTest {
        val post = generator.generate(idea, PostMood.WITTY, PostLength.MEDIUM, profileWith(200))
        assertTrue(Graphemes.count(post.composedText) in 1..200)
        assertEquals(Graphemes.count(post.composedText), post.characterCount)
    }

    @Test
    fun `분위기·내 글 반영·글자수 조합 전반에서 목표를 넘지 않는다`() = runTest {
        for (mood in PostMood.entries) {
            for (length in PostLength.entries) {
                for (count in listOf(50, 100, 180, 250, 500)) {
                    val post = generator.generate(idea, mood, length, profileWith(count))
                    val actual = Graphemes.count(post.composedText)
                    assertTrue("mood=$mood length=$length count=$count actual=$actual", actual in 1..count)
                }
            }
        }
    }

    @Test
    fun `같은 입력이면 항상 같은 결과가 나온다`() = runTest {
        val first = generator.generate(idea, PostMood.CALM, PostLength.LONG, profileWith(200))
        val second = generator.generate(idea, PostMood.CALM, PostLength.LONG, profileWith(200))
        assertEquals(first.composedText, second.composedText)
        assertEquals(first.hashtags, second.hashtags)
    }

    @Test
    fun `생성 결과는 저장된 형식 규칙을 통과한다`() = runTest {
        val profile = profileWith(200)
        val post = generator.generate(idea, PostMood.WITTY, PostLength.MEDIUM, profile)
        val report = post.formatReport
        assertTrue(report.failedRuleDescriptions.joinToString(), report.passesAllRules)

        val validation = CaptionValidationReport.evaluate(
            post.composedText,
            ExternalPromptBuilder.validationContext(profile)
        )
        assertTrue(validation.failedRuleDescriptions.joinToString(), validation.passesAllRules)
    }

    @Test
    fun `이모지 안 씀 프로필도 검증 규칙을 통과한다`() = runTest {
        val profile = profileWith(200, emojiIntensity = EmojiIntensity.NONE)
        val post = generator.generate(idea, PostMood.CALM, PostLength.SHORT, profile)
        assertTrue(post.characterCount in 1..200)
        val validation = CaptionValidationReport.evaluate(
            post.composedText,
            ExternalPromptBuilder.validationContext(profile)
        )
        assertTrue(validation.failedRuleDescriptions.joinToString(), validation.passesAllRules)
    }

    @Test
    fun `첫 줄은 사용자의 핵심 내용을 보존한다`() = runTest {
        val post = generator.generate(idea, PostMood.WITTY, PostLength.MEDIUM, profileWith(200))
        val firstLine = post.composedText.split("\n").first()
        assertTrue(firstLine.startsWith("오늘 아침 한강"))
    }

    @Test
    fun `금지 표현이 들어간 문장은 본문에서 제외된다`() = runTest {
        val profile = profileWith(300).copy(prohibitedPhrases = "승자")
        val post = generator.generate(idea, PostMood.WITTY, PostLength.MEDIUM, profile)
        assertTrue(!post.composedText.contains("승자"))
    }

    @Test
    fun `sanitize는 따옴표·해시·이모지·문장부호를 정리한다`() {
        val raw = "\"오늘\" #한강… 러닝! ✨끝?"
        assertEquals("오늘 한강 러닝 끝", DeterministicCaptionGenerator.sanitize(raw))
    }

    @Test
    fun `flexSentence는 요청한 길이를 정확히 만든다`() {
        for (target in 1..40) {
            val seed = DeterministicCaptionGenerator.Companion.SeedBox(
                DeterministicCaptionGenerator.seed("길이$target")
            )
            val sentence = DeterministicCaptionGenerator.flexSentence(target, seed)
            assertEquals("target=$target", target, Graphemes.count(sentence))
        }
    }

    @Test
    fun `시드는 Swift FNV-1a 오버플로 동작을 따른다`() {
        // FNV-1a 64bit 표준 벡터: "a" -> 0xaf63dc4c8601ec8c
        assertEquals(0xaf63dc4c8601ec8cuL, DeterministicCaptionGenerator.seed("a"))
        // 빈 문자열은 오프셋 그대로
        assertEquals(0xcbf29ce484222325uL, DeterministicCaptionGenerator.seed(""))
    }
}
