package com.armsone.starmanager

import com.armsone.starmanager.model.CaptionFormatReport
import com.armsone.starmanager.model.CaptionValidationContext
import com.armsone.starmanager.model.CaptionValidationReport
import com.armsone.starmanager.text.Graphemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionReportsTest {

    /** 형식 규칙을 모두 지키는 짧은 예시(글자 수는 동적으로 맞춰 검사). */
    private val compliant = listOf(
        "#오늘 #한강",
        "🌿강바람이 좋았다.",
        "천천히 걷다 보니 마음도 느려졌다.",
        "🌙 고요하게 채운 하루의 기록 🌙"
    ).joinToString("\n")

    private fun requiredCount(text: String) = Graphemes.count(text)

    @Test
    fun `규칙을 지키면 통과한다`() {
        val report = CaptionFormatReport.evaluate(compliant, requiredCount(compliant))
        assertTrue(report.failedRuleDescriptions.joinToString(), report.passesAllRules)
    }

    @Test
    fun `글자 수가 다르면 실패하고 설명에 현재 글자 수가 담긴다`() {
        val report = CaptionFormatReport.evaluate(compliant, requiredCount(compliant) + 1)
        assertFalse(report.passesAllRules)
        assertTrue(report.failedRuleDescriptions.first().contains("공백 포함"))
        assertTrue(report.failedRuleDescriptions.first().contains("현재 ${requiredCount(compliant)}자"))
    }

    @Test
    fun `첫 줄 해시태그 규칙 위반을 잡아낸다`() {
        val bad = compliant.replaceFirst("#오늘 #한강", "#오늘 #Han강")
        val report = CaptionFormatReport.evaluate(bad, requiredCount(bad))
        assertFalse(report.firstLineHasTwoKoreanHashtags)

        val single = compliant.replaceFirst("#오늘 #한강", "#오늘")
        assertFalse(
            CaptionFormatReport.evaluate(single, requiredCount(single)).firstLineHasTwoKoreanHashtags
        )
    }

    @Test
    fun `문장 중간 마침표를 잡아낸다`() {
        val bad = compliant.replace("좋았다.", "좋았다. 정말")
        val report = CaptionFormatReport.evaluate(bad, requiredCount(bad))
        assertFalse(report.periodsAlwaysEndLines)
    }

    @Test
    fun `전체 따옴표를 잡아낸다`() {
        val bad = "\"$compliant\""
        val report = CaptionFormatReport.evaluate(bad, requiredCount(bad))
        assertFalse(report.hasNoFullTextQuotes)
    }

    @Test
    fun `첫 줄이나 문장 중간 이모지를 잡아낸다`() {
        val firstLineEmoji = compliant.replaceFirst("#오늘 #한강", "#오늘 #한강🌿")
        assertFalse(
            CaptionFormatReport.evaluate(firstLineEmoji, requiredCount(firstLineEmoji))
                .emojiUsageIsRestrained
        )
        val midEmoji = compliant.replace("걷다 보니", "걷다🌿 보니")
        assertFalse(
            CaptionFormatReport.evaluate(midEmoji, requiredCount(midEmoji)).emojiUsageIsRestrained
        )
    }

    @Test
    fun `마지막 줄 요약은 이모지로 감싸야 한다`() {
        val bad = compliant.replace("🌙 고요하게 채운 하루의 기록 🌙", "고요하게 채운 하루의 기록")
        val report = CaptionFormatReport.evaluate(bad, requiredCount(bad))
        assertFalse(report.lastLineIsEmojiWrappedSummary)
    }

    @Test
    fun `검증 리포트는 금지 표현을 대소문자 무시로 찾는다`() {
        val context = CaptionValidationContext(
            requiredCharacterCount = requiredCount(compliant),
            prohibitedPhrases = "강바람, Nothing;여기없음",
            allowsBodyEmoji = true
        )
        val report = CaptionValidationReport.evaluate(compliant, context)
        assertEquals(listOf("강바람"), report.prohibitedPhraseMatches)
        assertFalse(report.passesAllRules)
        assertTrue(report.failedRuleDescriptions.any { it.contains("금지 표현 제외") })
    }

    @Test
    fun `본문 이모지 설정을 검사한다`() {
        val context = CaptionValidationContext(
            requiredCharacterCount = requiredCount(compliant),
            prohibitedPhrases = "",
            allowsBodyEmoji = false
        )
        val report = CaptionValidationReport.evaluate(compliant, context)
        // 본문(🌿강바람...)에 이모지가 있으므로 위반
        assertFalse(report.respectsBodyEmojiPreference)
    }

    @Test
    fun `글자 수 표기와 최소 문단 수를 검사한다`() {
        val withLabel = compliant.replace("강바람이 좋았다.", "강바람이 좋았다 200자.")
        val context = CaptionValidationContext(
            requiredCharacterCount = requiredCount(withLabel),
            prohibitedPhrases = "",
            allowsBodyEmoji = true
        )
        assertFalse(CaptionValidationReport.evaluate(withLabel, context).hasNoCharacterCountLabel)

        val short = "#오늘 #한강\n🌙 요약 🌙"
        val shortContext = CaptionValidationContext(
            requiredCharacterCount = requiredCount(short),
            prohibitedPhrases = "",
            allowsBodyEmoji = true
        )
        assertFalse(CaptionValidationReport.evaluate(short, shortContext).hasMinimumLineCount)
    }

    @Test
    fun `문자소 계산은 결합 이모지를 한 글자로 센다`() {
        assertEquals(1, Graphemes.count("👨‍👩‍👧"))
        assertEquals(4, Graphemes.count("한글🧡!"))
        assertEquals("한글", Graphemes.prefix("한글🧡!", 2))
        assertTrue(Graphemes.isEmojiCluster("🧡"))
        assertTrue(Graphemes.isEmojiCluster("✨"))
        assertFalse(Graphemes.isEmojiCluster("가"))
        assertTrue(Graphemes.isHangulCluster("가"))
        assertFalse(Graphemes.isHangulCluster("a"))
    }
}
