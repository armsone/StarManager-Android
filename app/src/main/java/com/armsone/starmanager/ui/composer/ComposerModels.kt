package com.armsone.starmanager.ui.composer

import com.armsone.starmanager.model.CreatorProfile
import com.armsone.starmanager.model.GeneratedPost
import com.armsone.starmanager.model.PostLength
import com.armsone.starmanager.model.PostMood
import com.armsone.starmanager.service.DirectAIProvider
import java.util.UUID

/** iOS PreviewAspect — 게시 비율 3종. */
enum class PreviewAspect(val title: String, val ratio: Float) {
    SQUARE("1:1", 1f),
    FEED("4:5", 4f / 5f),
    VERTICAL("9:16", 9f / 16f)
}

/** 생성 당시 조건 스냅샷 — 조건이 바뀌면 초안을 만료 처리한다. */
data class DraftSignature(
    val idea: String,
    val mood: PostMood,
    val length: PostLength,
    val profile: CreatorProfile
)

/** 문구 출처 — 비교 카드와 미리보기 라벨에 사용. */
enum class CaptionSource {
    DEVICE,
    DETERMINISTIC,
    CHAT_GPT,
    GEMINI,
    GROK;

    val title: String
        get() = when (this) {
            DEVICE -> "아이폰 AI"
            DETERMINISTIC -> "기본 생성"
            CHAT_GPT -> "ChatGPT"
            GEMINI -> "Gemini"
            GROK -> "Grok"
        }
}

val DirectAIProvider.captionSource: CaptionSource
    get() = when (this) {
        DirectAIProvider.OPEN_AI -> CaptionSource.CHAT_GPT
        DirectAIProvider.GEMINI -> CaptionSource.GEMINI
        DirectAIProvider.GROK -> CaptionSource.GROK
    }

data class CaptionCandidate(
    val source: CaptionSource,
    val post: GeneratedPost,
    val signature: DraftSignature,
    val requestId: String = UUID.randomUUID().toString()
)

enum class MediaKind {
    IMAGE,
    VIDEO;

    val title: String get() = if (this == IMAGE) "사진" else "영상"
}

class ComposerMedia(
    val data: ByteArray,
    val kind: MediaKind,
    val fileExtension: String?,
    val generatedFromPostId: String? = null,
    val id: String = UUID.randomUUID().toString()
)

/** 만들기 카드의 AI 선택 버튼 — AI(기기) + 외부 3사. */
sealed class AIChoice(val id: String, val title: String) {
    data object OnDevice : AIChoice("apple-ai", "AI")
    data class External(val provider: DirectAIProvider) : AIChoice(provider.rawValue, provider.title)

    companion object {
        val all: List<AIChoice> = listOf(
            OnDevice,
            External(DirectAIProvider.OPEN_AI),
            External(DirectAIProvider.GEMINI),
            External(DirectAIProvider.GROK)
        )
    }
}
