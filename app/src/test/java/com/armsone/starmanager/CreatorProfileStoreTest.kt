package com.armsone.starmanager

import com.armsone.starmanager.model.AppAppearance
import com.armsone.starmanager.model.CreatorProfile
import com.armsone.starmanager.model.CreatorProfileStore
import com.armsone.starmanager.model.GenerationControls
import com.armsone.starmanager.model.GenerationStylePreset
import com.armsone.starmanager.model.InMemoryKeyValueStore
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.model.WritingPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorProfileStoreTest {

    @Test
    fun `최초 실행 시 386 스타일이 적용되고 200자는 유지된다`() {
        val store = CreatorProfileStore(InMemoryKeyValueStore())
        val profile = store.profile.value
        assertEquals(200, profile.controls.characterCount)
        assertEquals("살아본 사람의 현실감은 살리되 정답을 강요하지 않고 유쾌하게", profile.voice)
        assertFalse(profile.usesEmoji)
        assertEquals(GenerationControls(200, 20, 25, 15, 25, 15), profile.controls)
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
    fun `분위기와 이야기 비중 설정이 영속화된다`() {
        val storage = InMemoryKeyValueStore()
        val store = CreatorProfileStore(storage)
        store.updateProfile { it.copy(mood = PostMood.WARM, preferredLength = PostLength.SHORT) }

        val reloaded = CreatorProfileStore(storage)
        assertEquals(PostMood.WARM, reloaded.profile.value.mood)
        assertEquals(PostLength.SHORT, reloaded.profile.value.preferredLength)
    }

    @Test
    fun `스타일 프리셋 적용 시 selectedGenerationStyle이 갱신된다`() {
        val storage = InMemoryKeyValueStore()
        val store = CreatorProfileStore(storage)
        store.applyGenerationStyle(GenerationStylePreset.MZ)

        assertEquals(GenerationStylePreset.MZ, store.profile.value.selectedGenerationStyle)
        val reloaded = CreatorProfileStore(storage)
        assertEquals(GenerationStylePreset.MZ, reloaded.profile.value.selectedGenerationStyle)
    }

    @Test
    fun `프리셋 저장 및 적용 시 분위기와 스타일이 보존된다`() {
        val storage = InMemoryKeyValueStore()
        val store = CreatorProfileStore(storage)
        store.updateProfile {
            it.copy(
                mood = PostMood.CALM,
                preferredLength = PostLength.LONG,
                selectedGenerationStyle = GenerationStylePreset.GEN_X
            )
        }
        store.savePreset("X세대 차분한 톤")

        val preset = store.presets.value.first { it.name == "X세대 차분한 톤" }
        assertEquals(PostMood.CALM, preset.mood)
        assertEquals(GenerationStylePreset.GEN_X, preset.selectedGenerationStyle)

        // 프로필 변경 후 프리셋 적용
        store.updateProfile { it.copy(mood = PostMood.WITTY, selectedGenerationStyle = GenerationStylePreset.MZ) }
        store.apply(preset)

        assertEquals(PostMood.CALM, store.profile.value.mood)
        assertEquals(GenerationStylePreset.GEN_X, store.profile.value.selectedGenerationStyle)
    }

    @Test
    fun `마이그레이션은 저장된 글자 수를 보존한다`() {
        val storage = InMemoryKeyValueStore()
        // 버전 키 없이 350자 프로필이 저장돼 있던 상황을 재현
        val legacy = CreatorProfile(generationControls = GenerationControls(characterCount = 350))
        storage.putString(
            CreatorProfileStore.STORAGE_KEY,
            com.google.gson.Gson().toJson(legacy)
        )
        val store = CreatorProfileStore(storage)
        assertEquals(350, store.profile.value.controls.characterCount)
        assertEquals(
            GenerationStylePreset.GENERATION_386.applyingTo(legacy),
            store.profile.value
        )
    }

    @Test
    fun `마이그레이션은 한 번만 수행된다`() {
        val storage = InMemoryKeyValueStore()
        val first = CreatorProfileStore(storage)
        first.updateProfile { it.copy(voice = "직접 수정한 말투") }

        val second = CreatorProfileStore(storage)
        assertEquals("직접 수정한 말투", second.profile.value.voice)
    }

    @Test
    fun `기본 프리셋 3종이 제공된다`() {
        val store = CreatorProfileStore(InMemoryKeyValueStore())
        assertEquals(
            listOf("균형 잡힌 기본", "감성적인 기록", "참신하고 시크하게"),
            store.presets.value.map { it.name }
        )
        assertEquals(250, store.presets.value[1].controls.characterCount)
        assertEquals(180, store.presets.value[2].controls.characterCount)
    }

    @Test
    fun `프로필 변경은 즉시 저장된다`() {
        val storage = InMemoryKeyValueStore()
        val store = CreatorProfileStore(storage)
        store.updateProfile { it.copy(accountTopic = "새 주제") }
        val reloaded = CreatorProfileStore(storage)
        assertEquals("새 주제", reloaded.profile.value.accountTopic)
    }

    @Test
    fun `같은 이름으로 저장하면 프리셋이 교체된다`() {
        val store = CreatorProfileStore(InMemoryKeyValueStore())
        store.savePreset("나만의 톤")
        store.updateProfile { it.copy(accountTopic = "변경된 주제") }
        store.savePreset("나만의 톤")
        val saved = store.presets.value.filter { it.name == "나만의 톤" }
        assertEquals(1, saved.size)
        assertEquals("변경된 주제", saved.first().accountTopic)
    }

    @Test
    fun `공백 이름은 프리셋으로 저장되지 않는다`() {
        val store = CreatorProfileStore(InMemoryKeyValueStore())
        val before = store.presets.value.size
        store.savePreset("   ")
        assertEquals(before, store.presets.value.size)
    }

    @Test
    fun `프리셋 적용은 저장된 필드만 덮어쓴다`() {
        val store = CreatorProfileStore(InMemoryKeyValueStore())
        val preset = WritingPreset(
            name = "부분 프리셋",
            controls = GenerationControls(characterCount = 120),
            additionalInstructions = "프리셋 지침",
            voice = "프리셋 말투"
            // accountTopic 등 나머지는 null → 기존 값 유지
        )
        val topicBefore = store.profile.value.accountTopic
        store.apply(preset)
        assertEquals(120, store.profile.value.controls.characterCount)
        assertEquals("프리셋 말투", store.profile.value.voice)
        assertEquals("프리셋 지침", store.profile.value.additionalInstructions)
        assertEquals(topicBefore, store.profile.value.accountTopic)
    }

    @Test
    fun `프리셋 삭제와 지침 복원이 동작한다`() {
        val storage = InMemoryKeyValueStore()
        val store = CreatorProfileStore(storage)
        store.deletePreset(0)
        assertEquals(2, store.presets.value.size)
        assertEquals(2, CreatorProfileStore(storage).presets.value.size)

        store.updateProfile { it.copy(writingGuidelines = "수정된 지침") }
        store.restoreDefaultWritingGuidelines()
        assertEquals(CreatorProfile.DEFAULT_WRITING_GUIDELINES, store.profile.value.writingGuidelines)
        assertTrue(store.profile.value.writingGuidelines.contains("공백 포함 200자 정확히 준수"))
    }
}
