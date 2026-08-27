package com.armsone.starmanager

import com.armsone.starmanager.model.CreatorProfile
import com.armsone.starmanager.model.GenerationControls
import com.armsone.starmanager.model.GenerationStylePreset
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.service.ExternalPromptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorProfilePromptTest {

    @Test
    fun `기본 프로필 값이 iOS와 일치한다`() {
        val profile = CreatorProfile()
        assertEquals("나의 일상과 경험", profile.accountTopic)
        assertEquals("다정하고 솔직하게", profile.voice)
        assertEquals("내 이야기에 공감하는 사람들", profile.audience)
        assertEquals(PostLength.MEDIUM, profile.preferredLength)
        assertTrue(profile.usesEmoji)
        assertEquals("", profile.prohibitedPhrases)
        assertEquals("핵심 키워드 중심", profile.hashtagStyle)
        assertEquals(GenerationControls(200, 20, 20, 30, 20, 10), profile.controls)
        assertEquals(100, profile.controls.toneTotal)
    }

    @Test
    fun `간결한 생성 프롬프트는 섹션 순서와 필수 지침을 준수한다`() {
        val profile = CreatorProfile(
            generationControls = GenerationControls(characterCount = 180),
            prohibitedPhrases = "최고, 대박",
            hashtagStyle = "일상 감성",
            additionalInstructions = "따뜻한 여운을 남겨줘"
        )
        val prompt = profile.generationPrompt("비 오는 날의 카페", PostMood.WARM, PostLength.SHORT)

        assertTrue(prompt.startsWith("[내가 입력한 내용]\n비 오는 날의 카페\n\n[원하는 결과]"))
        assertTrue(prompt.contains("- 공백과 줄바꿈을 포함해 정확히 180자로 작성"))
        assertTrue(prompt.contains("- 첫 줄에 한글 해시태그 2개 연속 작성"))
        assertTrue(prompt.contains("- 본문: 따뜻하게, 다정하고 솔직하게, 입력한 이야기의 핵심만 남기고 새로운 비유와 해석을 적극적으로 더할 것"))
        assertTrue(prompt.contains("- 문장마다 줄바꿈하고 상투적인 표현 없이 자연스럽게 작성"))
        assertTrue(prompt.contains("- 본문 이모지는 문단 앞쪽에만 절제해서 사용"))
        assertTrue(prompt.contains("- 마지막 줄은 전체 요약 1줄로 작성하고 앞뒤에 이모지 배치"))
        assertTrue(prompt.contains("- 금지 표현: 최고, 대박"))
        assertTrue(prompt.contains("- 해시태그 취향: 일상 감성"))
        assertTrue(prompt.endsWith("[추가 요청]\n따뜻한 여운을 남겨줘"))
    }

    @Test
    fun `추가 요청이 없으면 추가 요청 섹션을 생략한다`() {
        val profile = CreatorProfile(
            usesEmoji = false,
            additionalInstructions = "   "
        )
        val prompt = profile.generationPrompt("산책길", PostMood.CALM, PostLength.MEDIUM)

        assertFalse(prompt.contains("[추가 요청]"))
        assertTrue(prompt.contains("- 마지막 요약 줄의 필수 이모지를 제외하고 본문 이모지는 사용하지 않음"))
    }

    @Test
    fun `ExternalPromptBuilder는 간결한 생성 프롬프트를 조립한다`() {
        val profile = CreatorProfile(generationControls = GenerationControls(characterCount = 150))
        val prompt = ExternalPromptBuilder.build(profile, "아이디어", PostMood.WARM, PostLength.LONG)

        assertTrue(prompt.startsWith("[내가 입력한 내용]\n아이디어\n\n[원하는 결과]"))
        assertTrue(prompt.contains("- 본문: 따뜻하게, 다정하고 솔직하게, 입력한 이야기의 장면과 표현을 최대한 많이 살리고 과도한 각색은 줄일 것"))
        assertTrue(prompt.contains("- 공백과 줄바꿈을 포함해 정확히 150자로 작성"))
    }

    @Test
    fun `스타일 프리셋은 글자 수를 유지한 채 톤을 바꾼다`() {
        val base = CreatorProfile(generationControls = GenerationControls(characterCount = 440))
        for (preset in GenerationStylePreset.entries) {
            val applied = preset.applyingTo(base)
            assertEquals(preset.name, 440, applied.controls.characterCount)
            assertEquals(preset.name, 100, applied.controls.toneTotal)
        }
        val mz = GenerationStylePreset.MZ.applyingTo(base)
        assertTrue(mz.usesEmoji)
        val boomer = GenerationStylePreset.BABY_BOOM.applyingTo(base)
        assertFalse(boomer.usesEmoji)
        assertEquals(GenerationControls(440, 30, 35, 10, 15, 10), boomer.controls)
    }

    @Test
    fun `스타일 4종·분위기 3종·비중 3종이 정의돼 있다`() {
        assertEquals(4, GenerationStylePreset.entries.size)
        assertEquals(listOf("MZ", "X", "386", "꼰대"), GenerationStylePreset.entries.map { it.title })
        assertEquals(3, PostMood.entries.size)
        assertEquals(listOf("따뜻하게", "재치 있게", "담백하게"), PostMood.entries.map { it.rawValue })
        assertEquals(3, PostLength.entries.size)
        assertEquals(listOf("낮게", "보통", "높게"), PostLength.entries.map { it.storyWeightTitle })
    }
}
