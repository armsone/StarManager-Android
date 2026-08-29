package com.armsone.starmanager

import com.armsone.starmanager.model.AppAppearance
import com.armsone.starmanager.model.AudienceAgeGroup
import com.armsone.starmanager.model.CreatorProfile
import com.armsone.starmanager.model.CreatorProfileStore
import com.armsone.starmanager.model.EmojiIntensity
import com.armsone.starmanager.model.GenerationControls
import com.armsone.starmanager.model.InMemoryKeyValueStore
import com.armsone.starmanager.model.LineBreakFrequency
import com.armsone.starmanager.model.PostDestination
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.model.PostStyle
import com.armsone.starmanager.model.PostTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorProfileStoreTest {

    @Test
    fun `최초 실행 시 기본 설정이 올바르게 로드된다`() {
        val store = CreatorProfileStore(InMemoryKeyValueStore())
        val profile = store.profile.value
        assertEquals(200, profile.controls.characterCount)
        assertEquals(PostDestination.INSTAGRAM, profile.destination)
        assertEquals(EmojiIntensity.LOW, profile.emojiIntensity)
        assertEquals(AudienceAgeGroup.XZ, profile.ageGroup)
        assertEquals(PostStyle.MEMO, profile.style)
        assertEquals(PostTone.KIND, profile.tone)
        assertEquals(LineBreakFrequency.MODERATE, profile.lineBreakFrequency)
        assertEquals(PostLength.MEDIUM, profile.preferredLength)
    }

    @Test
    fun `앱 외형 설정은 영속화된다`() {
        val storage = InMemoryKeyValueStore()
        val store = CreatorProfileStore(storage)
        assertEquals(AppAppearance.BK, store.appearance.value)

        store.setAppearance(AppAppearance.CLASSIC)
        assertEquals(AppAppearance.CLASSIC, store.appearance.value)
        assertEquals("CLASSIC", storage.getString("appAppearance"))

        val reloaded = CreatorProfileStore(storage)
        assertEquals(AppAppearance.CLASSIC, reloaded.appearance.value)

        reloaded.setAppearance(AppAppearance.BK)
        assertEquals("BK", storage.getString("appAppearance"))
    }

    @Test
    fun `브라우저 보기는 기본으로 꺼져 있고 선택하면 영속화된다`() {
        val storage = InMemoryKeyValueStore()
        val store = CreatorProfileStore(storage)
        assertFalse(store.showsExternalAIBrowser.value)

        store.setShowsExternalAIBrowser(true)
        assertTrue(store.showsExternalAIBrowser.value)
        assertEquals(1, storage.getInt(CreatorProfileStore.SHOW_EXTERNAL_AI_BROWSER_STORAGE_KEY, 0))
        assertTrue(CreatorProfileStore(storage).showsExternalAIBrowser.value)

        store.setShowsExternalAIBrowser(false)
        assertFalse(CreatorProfileStore(storage).showsExternalAIBrowser.value)
    }

    @Test
    fun `자동화 사용은 기본으로 꺼져 있고 선택하면 영속화된다`() {
        val storage = InMemoryKeyValueStore()
        val store = CreatorProfileStore(storage)
        assertFalse(store.automationEnabled.value)

        store.setAutomationEnabled(true)
        assertTrue(store.automationEnabled.value)
        assertEquals(1, storage.getInt(CreatorProfileStore.AUTOMATION_ENABLED_STORAGE_KEY, 0))
        assertTrue(CreatorProfileStore(storage).automationEnabled.value)

        store.setAutomationEnabled(false)
        assertFalse(CreatorProfileStore(storage).automationEnabled.value)
    }

    @Test
    fun `프로필 설정 변경 시 영속화되고 destination에 맞춰 글자 수가 자동 clamp된다`() {
        val storage = InMemoryKeyValueStore()
        val store = CreatorProfileStore(storage)

        // 1. 카카오톡으로 변경 시 최대 200자로 clamping
        store.updateProfile {
            it.copy(
                destination = PostDestination.KAKAO_TALK,
                generationControls = GenerationControls(characterCount = 450),
                emojiIntensity = EmojiIntensity.HIGH,
                style = PostStyle.ESSAY,
                tone = PostTone.CHIC,
                ageGroup = AudienceAgeGroup.KKONDAE,
                lineBreakFrequency = LineBreakFrequency.FREQUENT,
                accountTopic = "개인 블로그",
                detailedGuidelines = "간결하게 작성"
            )
        }

        val clampedProfile = store.profile.value
        assertEquals(PostDestination.KAKAO_TALK, clampedProfile.destination)
        assertEquals(200, clampedProfile.controls.characterCount)
        assertEquals(EmojiIntensity.HIGH, clampedProfile.emojiIntensity)
        assertEquals(PostStyle.ESSAY, clampedProfile.style)
        assertEquals(PostTone.CHIC, clampedProfile.tone)
        assertEquals(AudienceAgeGroup.KKONDAE, clampedProfile.ageGroup)
        assertEquals(LineBreakFrequency.FREQUENT, clampedProfile.lineBreakFrequency)

        // 2. 저장소에서 다시 로드해도 보존됨
        val reloaded = CreatorProfileStore(storage)
        val reloadedProfile = reloaded.profile.value
        assertEquals(PostDestination.KAKAO_TALK, reloadedProfile.destination)
        assertEquals(200, reloadedProfile.controls.characterCount)
        assertEquals(EmojiIntensity.HIGH, reloadedProfile.emojiIntensity)
        assertEquals(PostStyle.ESSAY, reloadedProfile.style)
        assertEquals(PostTone.CHIC, reloadedProfile.tone)
        assertEquals(AudienceAgeGroup.KKONDAE, reloadedProfile.ageGroup)
        assertEquals(LineBreakFrequency.FREQUENT, reloadedProfile.lineBreakFrequency)
        assertEquals("개인 블로그", reloadedProfile.accountTopic)
        assertEquals("간결하게 작성", reloadedProfile.detailedGuidelines)
    }

    @Test
    fun `레거시 JSON 마이그레이션이 정상 수행된다`() {
        val storage = InMemoryKeyValueStore()
        // usesEmoji = false, 레거시 기본 topic이 들어있던 레거시 JSON 시뮬레이션
        val legacyJson = """
            {
                "accountTopic": "나의 일상과 경험",
                "voice": "다정하고 솔직하게",
                "audience": "내 이야기에 공감하는 사람들",
                "preferredLength": "MEDIUM",
                "usesEmoji": false,
                "prohibitedPhrases": "",
                "hashtagStyle": "핵심 키워드 중심",
                "writingGuidelines": "",
                "generationControls": {
                    "characterCount": 350
                }
            }
        """.trimIndent()
        storage.putString(CreatorProfileStore.STORAGE_KEY, legacyJson)

        val store = CreatorProfileStore(storage)
        val profile = store.profile.value

        // usesEmoji = false -> EmojiIntensity.NONE
        assertEquals(EmojiIntensity.NONE, profile.emojiIntensity)
        // 레거시 기본 텍스트들은 빈 문자열로 정제됨
        assertEquals("", profile.accountTopic)
        assertEquals("", profile.audience)
        assertEquals("", profile.hashtagStyle)
        // 글자 수 350자는 보존됨 (기본 Instagram limit 2200 -> max 500 내에 있으므로)
        assertEquals(350, profile.controls.characterCount)
    }
}
