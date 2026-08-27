package com.armsone.starmanager.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UserDefaults 대응. JVM 테스트에서는 인메모리 구현으로 대체한다. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun remove(key: String)
}

class SharedPreferencesKeyValueStore(context: Context) : KeyValueStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("starmanager", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    override fun remove(key: String) = prefs.edit().remove(key).apply()
}

class InMemoryKeyValueStore : KeyValueStore {
    private val strings = mutableMapOf<String, String>()
    private val ints = mutableMapOf<String, Int>()

    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String) { strings[key] = value }
    override fun getInt(key: String, default: Int): Int = ints[key] ?: default
    override fun putInt(key: String, value: Int) { ints[key] = value }
    override fun remove(key: String) { strings.remove(key); ints.remove(key) }
}

/** 앱 외형 테마: BK(기본)와 클래식 2가지 선택지 제공. */
enum class AppAppearance(val title: String) {
    BK("BK"),
    CLASSIC("클래식");

    companion object {
        fun fromString(value: String?): AppAppearance = when (value?.uppercase()) {
            "CLASSIC", "클래식" -> CLASSIC
            else -> BK
        }
    }
}

/**
 * iOS CreatorProfileStore 포팅.
 * 프로필과 프리셋은 변경 즉시 저장되고, 최초 실행(또는 버전 미달) 시
 * 386 스타일을 기본 적용하되 글자 수 200자는 유지한다.
 */
class CreatorProfileStore(private val storage: KeyValueStore) {

    private val gson = Gson()

    private val _appearance: MutableStateFlow<AppAppearance>
    val appearance: StateFlow<AppAppearance> get() = _appearance.asStateFlow()

    private val _showsExternalAIBrowser: MutableStateFlow<Boolean>
    val showsExternalAIBrowser: StateFlow<Boolean> get() = _showsExternalAIBrowser.asStateFlow()

    private val _profile: MutableStateFlow<CreatorProfile>
    val profile: StateFlow<CreatorProfile> get() = _profile.asStateFlow()

    private val _presets: MutableStateFlow<List<WritingPreset>>
    val presets: StateFlow<List<WritingPreset>> get() = _presets.asStateFlow()

    init {
        val storedAppearance = storage.getString(APPEARANCE_STORAGE_KEY)
        _appearance = MutableStateFlow(AppAppearance.fromString(storedAppearance))
        _showsExternalAIBrowser = MutableStateFlow(
            storage.getInt(SHOW_EXTERNAL_AI_BROWSER_STORAGE_KEY, 0) == 1
        )
        val storedPresets = storage.getString(PRESETS_STORAGE_KEY)?.let { json ->
            runCatching {
                gson.fromJson<List<WritingPreset>>(
                    json, object : TypeToken<List<WritingPreset>>() {}.type
                )
            }.getOrNull()
        }
        _presets = MutableStateFlow(storedPresets ?: WritingPreset.defaults)

        val storedProfile = storage.getString(STORAGE_KEY)?.let { json ->
            runCatching { gson.fromJson(json, CreatorProfile::class.java) }.getOrNull()
        } ?: CreatorProfile()

        if (storage.getInt(DEFAULT_STYLE_VERSION_KEY, 0) < CURRENT_DEFAULT_STYLE_VERSION) {
            val migrated = GenerationStylePreset.GENERATION_386.applyingTo(storedProfile)
            _profile = MutableStateFlow(migrated)
            storage.putInt(DEFAULT_STYLE_VERSION_KEY, CURRENT_DEFAULT_STYLE_VERSION)
            storage.putString(STORAGE_KEY, gson.toJson(migrated))
        } else {
            _profile = MutableStateFlow(storedProfile)
        }
    }

    fun updateProfile(transform: (CreatorProfile) -> CreatorProfile) {
        val updated = transform(_profile.value)
        _profile.value = updated
        save()
    }

    fun setProfile(profile: CreatorProfile) = updateProfile { profile }

    fun restoreDefaultWritingGuidelines() = updateProfile {
        it.copy(writingGuidelines = CreatorProfile.DEFAULT_WRITING_GUIDELINES)
    }

    fun applyGenerationStyle(preset: GenerationStylePreset) {
        setProfile(preset.applyingTo(_profile.value))
    }

    fun savePreset(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) return
        val current = _profile.value
        val preset = WritingPreset(
            name = name,
            controls = current.controls,
            additionalInstructions = current.additionalInstructions ?: "",
            writingGuidelines = current.writingGuidelines,
            accountTopic = current.accountTopic,
            voice = current.voice,
            audience = current.audience,
            preferredLength = current.preferredLength,
            usesEmoji = current.usesEmoji,
            prohibitedPhrases = current.prohibitedPhrases,
            hashtagStyle = current.hashtagStyle,
            mood = current.mood,
            selectedGenerationStyle = current.selectedGenerationStyle
        )
        _presets.value = _presets.value.filter { it.name != name } + preset
        savePresets()
    }

    fun apply(preset: WritingPreset) = updateProfile { profile ->
        var updated = profile.copy(
            generationControls = preset.controls,
            additionalInstructions = preset.additionalInstructions,
            writingGuidelines = preset.writingGuidelines
        )
        preset.accountTopic?.let { updated = updated.copy(accountTopic = it) }
        preset.voice?.let { updated = updated.copy(voice = it) }
        preset.audience?.let { updated = updated.copy(audience = it) }
        preset.preferredLength?.let { updated = updated.copy(preferredLength = it) }
        preset.usesEmoji?.let { updated = updated.copy(usesEmoji = it) }
        preset.prohibitedPhrases?.let { updated = updated.copy(prohibitedPhrases = it) }
        preset.hashtagStyle?.let { updated = updated.copy(hashtagStyle = it) }
        preset.mood?.let { updated = updated.copy(mood = it) }
        preset.selectedGenerationStyle?.let { updated = updated.copy(selectedGenerationStyle = it) }
        updated
    }

    fun deletePreset(index: Int) {
        val current = _presets.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _presets.value = current
            savePresets()
        }
    }

    fun deletePreset(preset: WritingPreset) {
        val index = _presets.value.indexOfFirst { it.id == preset.id }
        if (index >= 0) deletePreset(index)
    }

    fun setAppearance(appearance: AppAppearance) {
        _appearance.value = appearance
        storage.putString(APPEARANCE_STORAGE_KEY, appearance.name)
    }

    fun setShowsExternalAIBrowser(showsBrowser: Boolean) {
        _showsExternalAIBrowser.value = showsBrowser
        storage.putInt(SHOW_EXTERNAL_AI_BROWSER_STORAGE_KEY, if (showsBrowser) 1 else 0)
    }

    private fun save() {
        storage.putString(STORAGE_KEY, gson.toJson(_profile.value))
    }

    private fun savePresets() {
        storage.putString(PRESETS_STORAGE_KEY, gson.toJson(_presets.value))
    }

    companion object {
        const val STORAGE_KEY = "creatorProfile"
        const val PRESETS_STORAGE_KEY = "writingPresets"
        const val DEFAULT_STYLE_VERSION_KEY = "defaultGenerationStyleVersion"
        const val CURRENT_DEFAULT_STYLE_VERSION = 1
        const val APPEARANCE_STORAGE_KEY = "appAppearance"
        const val SHOW_EXTERNAL_AI_BROWSER_STORAGE_KEY = "showsExternalAIBrowser"
    }
}
