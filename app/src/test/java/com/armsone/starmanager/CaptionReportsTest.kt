package com.armsone.starmanager

import com.armsone.starmanager.model.CaptionFormatReport
import com.armsone.starmanager.model.CaptionValidationContext
import com.armsone.starmanager.model.CaptionValidationReport
import com.armsone.starmanager.model.EmojiIntensity
import com.armsone.starmanager.text.Graphemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionReportsTest {

    private val sampleText = listOf(
        "#오늘 #한강",
        "강바람이 시원하게 불어오는 저녁.",
        "천천히 걸으며 하루를 정리했다."
    ).joinToString("\n")

    @Test
    fun `게시 기준 글자 수 이내이면 포맷 리포트를 통과한다`() {
        val report = CaptionFormatReport.evaluate(sampleText, destinationLimit = 2200)
        assertTrue(report.passesAllRules)
        assertTrue(report.isWithinDestinationLimit)
        assertEquals(0, report.failedRuleDescriptions.size)
        assertEquals(Graphemes.count(sampleText), report.characterCount)
    }

    @Test
    fun `게시 기준 글자 수를 초과하면 포맷 리포트가 실패한다`() {
        val report = CaptionFormatReport.evaluate(sampleText, destinationLimit = 10)
        assertFalse(report.passesAllRules)
        assertFalse(report.isWithinDestinationLimit)
        assertEquals(1, report.failedRuleDescriptions.size)
        assertTrue(report.failedRuleDescriptions.first().contains("게시 기준 10자 이내"))
    }

    @Test
    fun `금지 표현 설정은 비활성화되어 검증 리포트에 영향을 주지 않는다`() {
        val context = CaptionValidationContext(
            destinationLimit = 2200,
            prohibitedPhrases = "강바람, nothing;없는단어",
            emojiIntensity = EmojiIntensity.LOW
        )
        val report = CaptionValidationReport.evaluate(sampleText, context)
        assertEquals(emptyList<String>(), report.prohibitedPhraseMatches)
        assertTrue(report.passesAllRules)
    }

    @Test
    fun `이모지 안 씀 설정일 때 이모지가 있으면 검증 리포트가 실패한다`() {
        val textWithEmoji = "$sampleText 🌿"
        val contextNone = CaptionValidationContext(
            destinationLimit = 2200,
            prohibitedPhrases = "",
            emojiIntensity = EmojiIntensity.NONE
        )
        val reportNone = CaptionValidationReport.evaluate(textWithEmoji, contextNone)
        assertFalse(reportNone.respectsEmojiNonePreference)
        assertFalse(reportNone.passesAllRules)
        assertTrue(reportNone.failedRuleDescriptions.contains("이모지 안 씀 설정 준수"))

        val contextLow = CaptionValidationContext(
            destinationLimit = 2200,
            prohibitedPhrases = "",
            emojiIntensity = EmojiIntensity.LOW
        )
        val reportLow = CaptionValidationReport.evaluate(textWithEmoji, contextLow)
        assertTrue(reportLow.respectsEmojiNonePreference)
        assertTrue(reportLow.passesAllRules)
    }

    @Test
    fun `글자 수 표기 라벨이 포함되어 있으면 검증 리포트가 실패한다`() {
        val withCountLabel = "$sampleText (200자)"
        val context = CaptionValidationContext(
            destinationLimit = 2200,
            prohibitedPhrases = "",
            emojiIntensity = EmojiIntensity.LOW
        )
        val report = CaptionValidationReport.evaluate(withCountLabel, context)
        assertFalse(report.hasNoCharacterCountLabel)
        assertFalse(report.passesAllRules)
        assertTrue(report.failedRuleDescriptions.contains("글자 수 표기 금지"))
    }

    @Test
    fun `모든 규칙을 준수하면 검증 리포트를 통과한다`() {
        val context = CaptionValidationContext(
            destinationLimit = 2200,
            prohibitedPhrases = "금지단어1, 금지단어2",
            emojiIntensity = EmojiIntensity.HIGH
        )
        val report = CaptionValidationReport.evaluate(sampleText, context)
        assertTrue(report.passesAllRules)
        assertEquals(0, report.failedRuleDescriptions.size)
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
        assertFalse(Graphemes.containsEmoji("오늘 날씨 맑음"))
    }
}
