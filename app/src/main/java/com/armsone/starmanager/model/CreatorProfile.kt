package com.armsone.starmanager.model

import java.util.UUID

/**
 * 프로필 및 작성 취향 설정. iOS CreatorProfile.swift 포팅.
 * 기본값은 사용자의 자유 입력을 위해 비워두며, Typed Enum 기반으로 프롬프트를 구성한다.
 */
data class CreatorProfile(
    val accountTopic: String = "",
    val voice: String = "",
    val audience: String = "",
    val preferredLength: PostLength = PostLength.MEDIUM,
    val emojiIntensity: EmojiIntensity = EmojiIntensity.LOW,
    val prohibitedPhrases: String = "",
    val hashtagStyle: String = "",
    val detailedGuidelines: String = "",
    val destination: PostDestination = PostDestination.INSTAGRAM,
    val ageGroup: AudienceAgeGroup = AudienceAgeGroup.XZ,
    val style: PostStyle = PostStyle.MEMO,
    val tone: PostTone = PostTone.KIND,
    val lineBreakFrequency: LineBreakFrequency = LineBreakFrequency.MODERATE,
    val writingGuidelines: String = "",
    val generationControls: GenerationControls? = null,
    val additionalInstructions: String? = null,
    val mood: PostMood = PostMood.WITTY,
    val selectedGenerationStyle: GenerationStylePreset? = null
) {
    val controls: GenerationControls
        get() = generationControls ?: GenerationControls()

    val characterCountPromptInstruction: String
        get() {
            val target = controls.characterCount
            val tolerance = maxOf(5, target / 10)
            return "완성 문구를 ${target - tolerance}~${target + tolerance}자 사이로 써"
        }

    val usesEmoji: Boolean
        get() = emojiIntensity != EmojiIntensity.NONE

    fun withControls(newControls: GenerationControls): CreatorProfile =
        copy(generationControls = newControls)

    fun clampCharacterCountToDestinationLimit(): CreatorProfile {
        val limit = destination.characterLimit
        val maxAllowed = minOf(500, limit)
        val clamped = controls.characterCount.coerceIn(50, maxAllowed)
        return copy(generationControls = controls.copy(characterCount = clamped))
    }

    fun generationPrompt(
        idea: String,
        mood: PostMood = this.mood,
        length: PostLength = this.preferredLength
    ): String {
        val trimmed = idea.trim()
        val parts = mutableListOf<String>()

        parts.add("[내가 입력한 내용]\n$trimmed")

        val resultLines = mutableListOf<String>()
        resultLines.add("위 내용을 바탕으로 한국어 글을 쓰고, 완성 문구만 출력해.")
        resultLines.add("- 글자 수: $characterCountPromptInstruction")
        resultLines.add("- 나잇대: ${ageGroup.promptAudienceHint}")
        resultLines.add("- 분위기: ${mood.rawValue}")
        resultLines.add("- 원문 반영: ${length.promptInstruction}")
        resultLines.add("- 이모지 사용: ${emojiIntensity.promptInstruction}")
        resultLines.add("- 스타일: ${style.promptInstruction}")
        resultLines.add("- 말투: ${tone.promptInstruction}")
        resultLines.add("- 줄넘김: ${lineBreakFrequency.promptInstruction}")

        if (detailedGuidelines.isNotBlank()) {
            resultLines.add("- 추가로 하고 싶은 설정: ${detailedGuidelines.trim()}")
        }
        if (!additionalInstructions.isNullOrBlank()) {
            resultLines.add("- 그 외 요청: ${additionalInstructions.trim()}")
        }

        parts.add("[원하는 결과]\n" + resultLines.joinToString("\n"))
        return parts.joinToString("\n\n")
    }

    fun photoOnlyPrompt(
        mood: PostMood = this.mood,
        length: PostLength = this.preferredLength,
        imageCount: Int = 1
    ): String {
        val parts = mutableListOf<String>()

        parts.add("[상황]\n선택한 사진 ${imageCount.coerceIn(1, 8)}장이 순서대로 첨부돼 있어. 사진을 모두 실제로 살펴보고, 사진에 없는 내용은 지어내지 마.")

        val resultLines = mutableListOf<String>()
        resultLines.add("사진 속 장면과 분위기를 바탕으로 한국어 글을 쓰고, 완성 문구만 출력해.")
        resultLines.add("- 글자 수: $characterCountPromptInstruction")
        resultLines.add("- 나잇대: ${ageGroup.promptAudienceHint}")
        resultLines.add("- 분위기: ${mood.rawValue}, ${length.promptInstruction}")
        resultLines.add("- 이모지 사용: ${emojiIntensity.promptInstruction}")
        resultLines.add("- 스타일: ${style.promptInstruction}")
        resultLines.add("- 말투: ${tone.promptInstruction}")
        resultLines.add("- 줄넘김: ${lineBreakFrequency.promptInstruction}")

        if (detailedGuidelines.isNotBlank()) {
            resultLines.add("- 추가로 하고 싶은 설정: ${detailedGuidelines.trim()}")
        }
        if (!additionalInstructions.isNullOrBlank()) {
            resultLines.add("- 그 외 요청: ${additionalInstructions.trim()}")
        }

        parts.add("[원하는 결과]\n" + resultLines.joinToString("\n"))
        return parts.joinToString("\n\n")
    }

    fun photoAndTextPrompt(
        idea: String,
        mood: PostMood = this.mood,
        length: PostLength = this.preferredLength,
        imageCount: Int = 1
    ): String {
        val trimmed = idea.trim()
        val parts = mutableListOf<String>()

        parts.add("[상황]\n선택한 사진 ${imageCount.coerceIn(1, 8)}장이 순서대로 첨부돼 있고 내가 적은 메모가 함께 있어. 사진을 모두 실제로 살펴보고, 사진과 메모 둘 다에 어울리는 글을 써 줘. 사진에 없는 내용은 지어내지 마.")
        parts.add("[내가 입력한 내용]\n$trimmed")

        val resultLines = mutableListOf<String>()
        resultLines.add("사진과 위 내용을 함께 반영한 한국어 글을 쓰고, 완성 문구만 출력해.")
        resultLines.add("- 글자 수: $characterCountPromptInstruction")
        resultLines.add("- 나잇대: ${ageGroup.promptAudienceHint}")
        resultLines.add("- 분위기: ${mood.rawValue}, ${length.promptInstruction}")
        resultLines.add("- 이모지 사용: ${emojiIntensity.promptInstruction}")
        resultLines.add("- 스타일: ${style.promptInstruction}")
        resultLines.add("- 말투: ${tone.promptInstruction}")
        resultLines.add("- 줄넘김: ${lineBreakFrequency.promptInstruction}")

        if (detailedGuidelines.isNotBlank()) {
            resultLines.add("- 추가로 하고 싶은 설정: ${detailedGuidelines.trim()}")
        }
        if (!additionalInstructions.isNullOrBlank()) {
            resultLines.add("- 그 외 요청: ${additionalInstructions.trim()}")
        }

        parts.add("[원하는 결과]\n" + resultLines.joinToString("\n"))
        return parts.joinToString("\n\n")
    }

    companion object {
        val DEFAULT_WRITING_GUIDELINES = ""
    }
}

/** 게시물 목적지 및 글자 수 한도 */
enum class PostDestination(
    val title: String,
    val characterLimit: Int,
    val limitBasisDescription: String
) {
    INSTAGRAM(
        title = "Instagram",
        characterLimit = 2200,
        limitBasisDescription = "Instagram 캡션 최대 2,200자 기준"
    ),
    KAKAO_TALK(
        title = "카카오톡",
        characterLimit = 200,
        limitBasisDescription = "카카오톡 공유(퍼가기) 메시지 문구 최대 200자 기준 (일반 채팅 글자 수 제한이 아님)"
    ),
    X(
        title = "X",
        characterLimit = 280,
        limitBasisDescription = "X 기본 게시물 최대 280자 기준 (Premium은 더 긴 게시물을 지원하지만 기본값은 280자)"
    )
}

/** 이모지 사용 강도 */
enum class EmojiIntensity(
    val title: String,
    val promptInstruction: String
) {
    NONE(
        title = "안씀",
        promptInstruction = "이모지를 전혀 사용하지 않는다"
    ),
    LOW(
        title = "최소한",
        promptInstruction = "꼭 필요한 곳에만 아주 가끔 이모지를 사용한다"
    ),
    HIGH(
        title = "적극적",
        promptInstruction = "문단마다 어울리는 이모지를 적극적으로 사용한다"
    ),
    HEAVY(
        title = "과하게",
        promptInstruction = "문장마다 이모지를 과감하게 여러 개 사용한다"
    )
}

/** 타깃 독자 나잇대 (프롬프트 전용 힌트) */
enum class AudienceAgeGroup(
    val title: String,
    val promptAudienceHint: String
) {
    XZ(
        title = "XZ",
        promptAudienceHint = "XZ세대(10~20대)가 편하게 느낄 감각으로"
    ),
    X(
        title = "X",
        promptAudienceHint = "X세대(40~50대)가 편하게 느낄 감각으로"
    ),
    THREE_EIGHT_SIX(
        title = "386",
        promptAudienceHint = "386세대(50~60대)가 편하게 느낄 감각으로"
    ),
    KKONDAE(
        title = "꼰대",
        promptAudienceHint = "꼰대 감성을 흉내 내는 재미있는 톤으로"
    )
}

/** 게시물 작성 스타일 */
enum class PostStyle(
    val title: String,
    val promptInstruction: String
) {
    MEMO(
        title = "메모",
        promptInstruction = "짧고 간결한 메모 형식으로"
    ),
    POEM(
        title = "시",
        promptInstruction = "시적인 형식으로, 행과 여백을 살려서"
    ),
    RAPPER(
        title = "랩퍼",
        promptInstruction = "리듬감 있는 랩 가사 형식으로"
    ),
    DIARY(
        title = "일기",
        promptInstruction = "그날의 일을 적는 일기 형식으로"
    ),
    ESSAY(
        title = "수필",
        promptInstruction = "생각과 경험을 풀어내는 수필 형식으로"
    ),
    NOVEL(
        title = "소설",
        promptInstruction = "소설처럼 장면과 서사가 있는 형식으로"
    )
}

/** 게시물 말투/어조 */
enum class PostTone(
    val title: String,
    val promptInstruction: String
) {
    CHIC(
        title = "시크하게",
        promptInstruction = "감정을 절제하고 쿨하게 시크한 말투로"
    ),
    FRESH(
        title = "참신하게",
        promptInstruction = "뻔하지 않고 참신한 표현을 쓰는 말투로"
    ),
    KIND(
        title = "친절하게",
        promptInstruction = "다정하고 친절한 말투로"
    )
}

/** 줄넘김 빈도 */
enum class LineBreakFrequency(
    val title: String,
    val promptInstruction: String
) {
    FREQUENT(
        title = "자주",
        promptInstruction = "짧은 문장마다 자주 줄을 바꿔서"
    ),
    MINIMAL(
        title = "최소",
        promptInstruction = "줄바꿈을 최소로 줄이고 문단을 길게 이어서"
    ),
    MODERATE(
        title = "적당히",
        promptInstruction = "문단 단위로 적당히 줄을 바꿔서"
    )
}

/** 원문 반영 정도 */
enum class PostLength(
    val rawValue: String,
    val storyWeightTitle: String,
    val storyWeightExplanation: String,
    val promptInstruction: String
) {
    SHORT(
        rawValue = "짧게",
        storyWeightTitle = "핵심만",
        storyWeightExplanation = "내가 쓴 글의 핵심만 남기고, 표현과 비유는 AI가 새로 씁니다",
        promptInstruction = "입력한 이야기의 핵심만 남기고 새로운 비유와 해석을 적극적으로 더할 것"
    ),
    MEDIUM(
        rawValue = "보통",
        storyWeightTitle = "균형 있게",
        storyWeightExplanation = "내가 쓴 글과 AI의 새 표현을 절반 정도씩 섞습니다",
        promptInstruction = "입력한 이야기와 새로운 해석을 균형 있게 섞을 것"
    ),
    LONG(
        rawValue = "길게",
        storyWeightTitle = "최대한 유지",
        storyWeightExplanation = "내가 쓴 문장과 표현을 최대한 그대로 살리고, AI는 다듬기만 합니다",
        promptInstruction = "입력한 이야기의 장면과 표현을 최대한 많이 살리고 과도한 각색은 줄일 것"
    )
}

/** 분위기 3종 */
enum class PostMood(val rawValue: String) {
    WARM("따뜻하게"),
    WITTY("재치 있게"),
    CALM("담백하게")
}

data class GenerationControls(
    val characterCount: Int = 200,
    val emotion: Int = 20,
    val kindness: Int = 20,
    val originality: Int = 30,
    val masculinity: Int = 20,
    val chic: Int = 10
) {
    val toneTotal: Int get() = emotion + kindness + originality + masculinity + chic

    fun swiftDescription(): String =
        "GenerationControls(characterCount: $characterCount, emotion: $emotion, " +
            "kindness: $kindness, originality: $originality, masculinity: $masculinity, chic: $chic)"
}

/** 레거시 스타일 프리셋 (하위 호환 유지용) */
enum class GenerationStylePreset(val rawValue: String) {
    MZ("mz"),
    GEN_X("genX"),
    GENERATION_386("generation386"),
    BABY_BOOM("babyBoom");

    val title: String
        get() = when (this) {
            MZ -> "MZ"
            GEN_X -> "X"
            GENERATION_386 -> "386"
            BABY_BOOM -> "꼰대"
        }

    val summary: String
        get() = when (this) {
            MZ -> "짧고 힙하게, 말맛은 톡톡"
            GEN_X -> "감성은 뜨겁게, 표현은 쿨하게"
            GENERATION_386 -> "경험은 묵직하게, 잔소리는 가볍게"
            BABY_BOOM -> "라떼는 진하게, 잔소리는 짧게"
        }

    fun applyingTo(profile: CreatorProfile): CreatorProfile {
        return when (this) {
            MZ -> profile.copy(
                ageGroup = AudienceAgeGroup.XZ,
                tone = PostTone.FRESH,
                style = PostStyle.MEMO,
                emojiIntensity = EmojiIntensity.HIGH,
                selectedGenerationStyle = MZ
            )
            GEN_X -> profile.copy(
                ageGroup = AudienceAgeGroup.X,
                tone = PostTone.CHIC,
                style = PostStyle.ESSAY,
                emojiIntensity = EmojiIntensity.NONE,
                selectedGenerationStyle = GEN_X
            )
            GENERATION_386 -> profile.copy(
                ageGroup = AudienceAgeGroup.THREE_EIGHT_SIX,
                tone = PostTone.KIND,
                style = PostStyle.DIARY,
                emojiIntensity = EmojiIntensity.NONE,
                selectedGenerationStyle = GENERATION_386
            )
            BABY_BOOM -> profile.copy(
                ageGroup = AudienceAgeGroup.KKONDAE,
                tone = PostTone.CHIC,
                style = PostStyle.NOVEL,
                emojiIntensity = EmojiIntensity.NONE,
                selectedGenerationStyle = BABY_BOOM
            )
        }
    }
}

/** 레거시 프리셋 데이터 구조 (하위 호환 및 보관용) */
data class WritingPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val controls: GenerationControls = GenerationControls(),
    val additionalInstructions: String = "",
    val writingGuidelines: String = "",
    val accountTopic: String? = null,
    val voice: String? = null,
    val audience: String? = null,
    val preferredLength: PostLength? = null,
    val usesEmoji: Boolean? = null,
    val prohibitedPhrases: String? = null,
    val hashtagStyle: String? = null,
    val mood: PostMood? = null,
    val selectedGenerationStyle: GenerationStylePreset? = null
) {
    companion object {
        val defaults: List<WritingPreset> = listOf(
            WritingPreset(name = "균형 잡힌 기본", controls = GenerationControls()),
            WritingPreset(
                name = "감성적인 기록",
                controls = GenerationControls(250)
            ),
            WritingPreset(
                name = "참신하고 시크하게",
                controls = GenerationControls(180)
            )
        )
    }
}
