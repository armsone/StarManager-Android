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
    fun `프롬프트는 고정 수치 지침을 빼고 현재 설정을 넣는다`() {
        val profile = CreatorProfile(
            generationControls = GenerationControls(320, 10, 20, 30, 25, 15),
            prohibitedPhrases = "대박",
            additionalInstructions = "  여운 있게  "
        )
        val prompt = profile.prompt("비 오는 날의 기록")

        assertFalse(prompt.contains("- 공백 포함 200자 정확히 준수"))
        assertFalse(prompt.contains("- 감동 20% / 친절함 20% / 참신함 30% / 남자다움 20% / 시크함 10%"))
        assertTrue(prompt.contains("- 공백 포함 320자 정확히 준수"))
        assertTrue(prompt.contains("- 감동 10% / 친절함 20% / 참신함 30% / 남자다움 25% / 시크함 15%"))
        assertTrue(prompt.contains("- 금지 표현: 대박"))
        assertTrue(prompt.contains("- 추가 옵션: 여운 있게"))
        assertTrue(prompt.endsWith("작성할 이야기:\n비 오는 날의 기록"))
    }

    @Test
    fun `금지 표현이 없으면 없음으로 표기하고 추가 옵션 줄은 비운다`() {
        val prompt = CreatorProfile().prompt("아이디어")
        assertTrue(prompt.contains("- 금지 표현: 없음"))
        assertFalse(prompt.contains("- 추가 옵션:"))
        assertTrue(prompt.contains("- 본문 이모지 사용: 문단 앞쪽에만 절제해서 사용"))
    }

    @Test
    fun `외부 AI 요청문에 분위기·비중·글자수 지시가 붙는다`() {
        val profile = CreatorProfile(generationControls = GenerationControls(characterCount = 150))
        val prompt = ExternalPromptBuilder.build(profile, "아이디어", PostMood.WARM, PostLength.LONG)
        assertTrue(prompt.contains("- 선택한 분위기: 따뜻하게"))
        assertTrue(
            prompt.contains("- 이야기 비중: 높게 — 입력한 이야기의 장면과 표현을 최대한 많이 살리고 과도한 각색은 줄일 것")
        )
        assertTrue(prompt.endsWith("- 공백과 줄바꿈을 포함해 정확히 150자로 작성"))
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
