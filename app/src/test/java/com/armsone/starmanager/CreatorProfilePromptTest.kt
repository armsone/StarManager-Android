package com.armsone.starmanager

import com.armsone.starmanager.model.AudienceAgeGroup
import com.armsone.starmanager.model.CreatorProfile
import com.armsone.starmanager.model.EmojiIntensity
import com.armsone.starmanager.model.GenerationControls
import com.armsone.starmanager.model.LineBreakFrequency
import com.armsone.starmanager.model.PostDestination
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.model.PostStyle
import com.armsone.starmanager.model.PostTone
import com.armsone.starmanager.service.ExternalPromptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorProfilePromptTest {

    @Test
    fun `기본 프로필 값이 iOS와 일치한다`() {
        val profile = CreatorProfile()
        assertEquals("", profile.accountTopic)
        assertEquals("", profile.audience)
        assertEquals(PostLength.MEDIUM, profile.preferredLength)
        assertEquals(EmojiIntensity.LOW, profile.emojiIntensity)
        assertEquals("", profile.prohibitedPhrases)
        assertEquals("", profile.hashtagStyle)
        assertEquals("", profile.detailedGuidelines)
        assertEquals(PostDestination.INSTAGRAM, profile.destination)
        assertEquals(AudienceAgeGroup.XZ, profile.ageGroup)
        assertEquals(PostStyle.MEMO, profile.style)
        assertEquals(PostTone.KIND, profile.tone)
        assertEquals(LineBreakFrequency.MODERATE, profile.lineBreakFrequency)
        assertEquals(GenerationControls(200, 20, 20, 30, 20, 10), profile.controls)
    }

    @Test
    fun `게시 대상별 글자 수 clamping이 올바르게 동작한다`() {
        // Instagram: limit 2200, clamp to 50..min(500, 2200) = 50..500
        val instagramProfile = CreatorProfile(
            destination = PostDestination.INSTAGRAM,
            generationControls = GenerationControls(characterCount = 600)
        ).clampCharacterCountToDestinationLimit()
        assertEquals(500, instagramProfile.controls.characterCount)

        val lowInstagramProfile = CreatorProfile(
            destination = PostDestination.INSTAGRAM,
            generationControls = GenerationControls(characterCount = 30)
        ).clampCharacterCountToDestinationLimit()
        assertEquals(50, lowInstagramProfile.controls.characterCount)

        // KakaoTalk: limit 200, clamp to 50..min(500, 200) = 50..200
        val kakaoProfile = CreatorProfile(
            destination = PostDestination.KAKAO_TALK,
            generationControls = GenerationControls(characterCount = 350)
        ).clampCharacterCountToDestinationLimit()
        assertEquals(200, kakaoProfile.controls.characterCount)

        // X: limit 280, clamp to 50..min(500, 280) = 50..280
        val xProfile = CreatorProfile(
            destination = PostDestination.X,
            generationControls = GenerationControls(characterCount = 300)
        ).clampCharacterCountToDestinationLimit()
        assertEquals(280, xProfile.controls.characterCount)
    }

    @Test
    fun `생성 프롬프트는 게시 대상, 나잇대, 이모지, 스타일, 말투, 줄넘김 지침을 포함한다`() {
        val profile = CreatorProfile(
            destination = PostDestination.INSTAGRAM,
            ageGroup = AudienceAgeGroup.XZ,
            style = PostStyle.ESSAY,
            tone = PostTone.CHIC,
            lineBreakFrequency = LineBreakFrequency.FREQUENT,
            emojiIntensity = EmojiIntensity.HIGH,
            generationControls = GenerationControls(characterCount = 180),
            accountTopic = "카페 투어",
            audience = "커피 애호가",
            prohibitedPhrases = "최고, 대박",
            hashtagStyle = "일상 감성",
            detailedGuidelines = "여운을 남겨줘"
        )
        val prompt = profile.generationPrompt("비 오는 날의 에스프레소", PostMood.WARM, PostLength.SHORT)

        assertTrue(prompt.startsWith("[내가 입력한 내용]\n비 오는 날의 에스프레소\n\n[원하는 결과]"))
        assertTrue(prompt.contains("한국어 글을 쓰고"))
        assertFalse(prompt.contains("Instagram에 올릴"))
        assertFalse(prompt.contains("- 게시 기준:"))
        assertTrue(prompt.contains("- 글자 수: 완성 문구를 162~198자 사이로 써"))
        assertTrue(prompt.contains("- 나잇대: ${AudienceAgeGroup.XZ.promptAudienceHint}"))
        assertTrue(prompt.contains("- 분위기: 따뜻하게"))
        assertTrue(prompt.contains("- 원문 반영: ${PostLength.SHORT.promptInstruction}"))
        assertTrue(prompt.contains("- 이모지 사용: ${EmojiIntensity.HIGH.promptInstruction}"))
        assertTrue(prompt.contains("- 스타일: ${PostStyle.ESSAY.promptInstruction}"))
        assertTrue(prompt.contains("- 말투: ${PostTone.CHIC.promptInstruction}"))
        assertTrue(prompt.contains("- 줄넘김: ${LineBreakFrequency.FREQUENT.promptInstruction}"))
        assertFalse(prompt.contains("- 주로 쓰는 주제"))
        assertFalse(prompt.contains("- 읽을 사람"))
        assertFalse(prompt.contains("- 금지 표현"))
        assertFalse(prompt.contains("- 해시태그 취향"))
        assertTrue(prompt.contains("- 추가로 하고 싶은 설정: 여운을 남겨줘"))
    }

    @Test
    fun `사진 단독 프롬프트와 사진 텍스트 결합 프롬프트가 올바르게 조립된다`() {
        val profile = CreatorProfile(
            destination = PostDestination.KAKAO_TALK,
            generationControls = GenerationControls(characterCount = 150)
        )

        val photoOnly = profile.photoOnlyPrompt(PostMood.WITTY, PostLength.MEDIUM)
        assertTrue(photoOnly.contains("[상황]\n대표 사진 한 장이 함께 첨부돼 있어."))
        assertTrue(photoOnly.contains("사진 속 장면과 분위기를 바탕으로 한국어 글을 쓰고"))
        assertTrue(photoOnly.contains("- 글자 수: 완성 문구를 135~165자 사이로 써"))
        assertFalse(photoOnly.contains("카카오톡에 올릴"))

        val photoAndText = profile.photoAndTextPrompt("오늘의 하늘", PostMood.CALM, PostLength.LONG)
        assertTrue(photoAndText.contains("[상황]\n대표 사진 한 장과 내가 적은 메모가 함께 있어."))
        assertTrue(photoAndText.contains("[내가 입력한 내용]\n오늘의 하늘"))
        assertTrue(photoAndText.contains("- 글자 수: 완성 문구를 135~165자 사이로 써"))
    }

    @Test
    fun `ExternalPromptBuilder는 CreatorProfile의 generationPrompt를 위임 호출한다`() {
        val profile = CreatorProfile(generationControls = GenerationControls(characterCount = 150))
        val prompt = ExternalPromptBuilder.build(profile, "아이디어", PostMood.WARM, PostLength.LONG)

        assertTrue(prompt.startsWith("[내가 입력한 내용]\n아이디어\n\n[원하는 결과]"))
        assertTrue(prompt.contains("한국어 글을 쓰고"))
        assertTrue(prompt.contains("- 글자 수: 완성 문구를 135~165자 사이로 써"))
    }

    @Test
    fun `모든 Enum의 항목 수와 한글 표기가 정확하다`() {
        assertEquals(3, PostDestination.entries.size)
        assertEquals(listOf("Instagram", "카카오톡", "X"), PostDestination.entries.map { it.title })

        assertEquals(4, EmojiIntensity.entries.size)
        assertEquals(listOf("안씀", "최소한", "적극적", "과하게"), EmojiIntensity.entries.map { it.title })

        assertEquals(3, PostMood.entries.size)
        assertEquals(listOf("따뜻하게", "재치 있게", "담백하게"), PostMood.entries.map { it.rawValue })

        assertEquals(6, PostStyle.entries.size)
        assertEquals(listOf("메모", "시", "랩퍼", "일기", "수필", "소설"), PostStyle.entries.map { it.title })

        assertEquals(3, PostTone.entries.size)
        assertEquals(listOf("시크하게", "참신하게", "친절하게"), PostTone.entries.map { it.title })

        assertEquals(4, AudienceAgeGroup.entries.size)
        assertEquals(listOf("XZ", "X", "386", "꼰대"), AudienceAgeGroup.entries.map { it.title })

        assertEquals(3, LineBreakFrequency.entries.size)
        assertEquals(listOf("자주", "최소", "적당히"), LineBreakFrequency.entries.map { it.title })

        assertEquals(3, PostLength.entries.size)
        assertEquals(listOf("핵심만", "균형 있게", "최대한 유지"), PostLength.entries.map { it.storyWeightTitle })
    }
}
