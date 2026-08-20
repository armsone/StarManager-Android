package com.armsone.starmanager.model

import java.util.UUID

/** iOS CreatorProfile.swift 포팅. 기본값과 프롬프트 조립을 그대로 유지한다. */
data class CreatorProfile(
    val accountTopic: String = "나의 일상과 경험",
    val voice: String = "다정하고 솔직하게",
    val audience: String = "내 이야기에 공감하는 사람들",
    val preferredLength: PostLength = PostLength.MEDIUM,
    val usesEmoji: Boolean = true,
    val prohibitedPhrases: String = "",
    val hashtagStyle: String = "핵심 키워드 중심",
    val writingGuidelines: String = DEFAULT_WRITING_GUIDELINES,
    val generationControls: GenerationControls? = null,
    val additionalInstructions: String? = null
) {
    val controls: GenerationControls
        get() = generationControls ?: GenerationControls()

    fun withControls(newControls: GenerationControls): CreatorProfile =
        copy(generationControls = newControls)

    fun prompt(idea: String): String {
        val activeGuidelines = writingGuidelines
            .split("\n")
            .filter { line ->
                !line.contains("공백 포함 200자") &&
                    !line.contains("감동 20% / 친절함 20%")
            }
            .joinToString("\n")
        val extra = (additionalInstructions ?: "").trim()
        return buildString {
            append(activeGuidelines)
            append("\n\n")
            append("아래 현재 설정을 기존 지침의 수치보다 최우선으로 적용:\n")
            append("- 공백 포함 ${controls.characterCount}자 정확히 준수\n")
            append("- 감동 ${controls.emotion}% / 친절함 ${controls.kindness}% / 참신함 ${controls.originality}% / 남자다움 ${controls.masculinity}% / 시크함 ${controls.chic}%\n")
            append("- 계정 주제: $accountTopic\n")
            append("- 주요 독자: $audience\n")
            append("- 나의 말투: $voice\n")
            append("- 본문 이모지 사용: ${if (usesEmoji) "문단 앞쪽에만 절제해서 사용" else "마지막 요약 줄의 필수 이모지를 제외하고 사용하지 않음"}\n")
            append("- 금지 표현: ${prohibitedPhrases.ifEmpty { "없음" }}\n")
            append("- 해시태그 취향: $hashtagStyle\n")
            append(if (extra.isEmpty()) "" else "- 추가 옵션: $extra")
            append("\n\n")
            append("작성할 이야기:\n")
            append(idea)
        }
    }

    companion object {
        val DEFAULT_WRITING_GUIDELINES = """
            인스타그램에 올릴 글로 작성해줘. 아래의 내용을 참고해
            - 결과만 출력
            - 인스타그램용 산문
            - 공백 포함 200자 정확히 준수
            - 감동 20% / 친절함 20% / 참신함 30% / 남자다움 20% / 시크함 10%
            - 혼잣말처럼 서술
            - 흔하지 않은 유의어 사용
            - 라임과 리듬 살릴 것
            - 문장 중간 따옴표 사용 가능
            - 전체 따옴표 사용 금지
            - 마침표 있으면 무조건 줄바꿈
            - 쉼표도 문맥에 맞게 가능하면 줄바꿈
            - 첫 줄에 한글 태그 2개 연속
            - 이모티콘은 첫줄 빼고 문단 앞쪽에만 절제해서 사용
            - 마지막 줄은 전체 요약 1줄 + 이모티콘 앞뒤 배치
            - 글자수 표기 금지
        """.trimIndent()
    }
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

    /** Swift String(describing:) 형식 — 결정적 시드 계산에 사용된다. */
    fun swiftDescription(): String =
        "GenerationControls(characterCount: $characterCount, emotion: $emotion, " +
            "kindness: $kindness, originality: $originality, masculinity: $masculinity, chic: $chic)"
}

/** iOS GenerationStylePreset — 4가지 스타일. */
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

    /** 글자 수는 유지하고 말투/이모지/추가 지침/톤 배분만 적용한다. */
    fun applyingTo(profile: CreatorProfile): CreatorProfile {
        val characterCount = profile.controls.characterCount
        return when (this) {
            MZ -> profile.copy(
                voice = "짧고 빠른 호흡으로, 눈치 빠른 한마디와 신선한 비유를 섞어 재치 있게",
                usesEmoji = true,
                additionalInstructions = "억지 유행어는 피하고 설명보다 장면, 장면보다 한 방 있는 말맛을 먼저 보여주기",
                generationControls = GenerationControls(characterCount, 15, 15, 45, 5, 20)
            )
            GEN_X -> profile.copy(
                voice = "속은 뜨겁지만 겉은 쿨하게, 낭만과 현실을 한 문장 안에서 교차시키며",
                usesEmoji = false,
                additionalInstructions = "과한 신파 없이 장면은 선명하게, 결론은 무심한 듯 멋있게 남기기",
                generationControls = GenerationControls(characterCount, 25, 15, 25, 15, 20)
            )
            GENERATION_386 -> profile.copy(
                voice = "살아본 사람의 현실감은 살리되 정답을 강요하지 않고 유쾌하게",
                usesEmoji = false,
                additionalInstructions = "성공담보다 시행착오를 앞세우고, 잔소리가 될 순간에는 자조적인 유머로 방향 틀기",
                generationControls = GenerationControls(characterCount, 20, 25, 15, 25, 15)
            )
            BABY_BOOM -> profile.copy(
                voice = "라떼 한 잔 같은 연륜을 깔고, 스스로도 웃을 줄 아는 능청스러운 꼰대 말투로",
                usesEmoji = false,
                additionalInstructions = "한 번쯤 훈계할 듯 운을 떼되 결론에서는 자기 흑역사를 꺼내 웃음과 쓸 만한 지혜를 함께 남기기",
                generationControls = GenerationControls(characterCount, 30, 35, 10, 15, 10)
            )
        }
    }
}

data class WritingPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val controls: GenerationControls,
    val additionalInstructions: String = "",
    val writingGuidelines: String = CreatorProfile.DEFAULT_WRITING_GUIDELINES,
    val accountTopic: String? = null,
    val voice: String? = null,
    val audience: String? = null,
    val preferredLength: PostLength? = null,
    val usesEmoji: Boolean? = null,
    val prohibitedPhrases: String? = null,
    val hashtagStyle: String? = null
) {
    companion object {
        val defaults: List<WritingPreset> = listOf(
            WritingPreset(name = "균형 잡힌 기본", controls = GenerationControls()),
            WritingPreset(
                name = "감성적인 기록",
                controls = GenerationControls(250, 40, 25, 20, 5, 10),
                additionalInstructions = "잔잔한 여운과 따뜻한 장면 묘사를 강조"
            ),
            WritingPreset(
                name = "참신하고 시크하게",
                controls = GenerationControls(180, 10, 10, 40, 15, 25),
                additionalInstructions = "군더더기 없이 낯선 비유와 짧은 호흡을 사용"
            )
        )
    }
}

/** iOS PostLength — 이야기 비중 3단계. */
enum class PostLength(val rawValue: String) {
    SHORT("짧게"),
    MEDIUM("보통"),
    LONG("길게");

    val storyWeightTitle: String
        get() = when (this) {
            SHORT -> "낮게"
            MEDIUM -> "보통"
            LONG -> "높게"
        }

    val promptInstruction: String
        get() = when (this) {
            SHORT -> "입력한 이야기의 핵심만 남기고 새로운 비유와 해석을 적극적으로 더할 것"
            MEDIUM -> "입력한 이야기와 새로운 해석을 균형 있게 섞을 것"
            LONG -> "입력한 이야기의 장면과 표현을 최대한 많이 살리고 과도한 각색은 줄일 것"
        }
}

/** iOS PostMood — 3가지 분위기. */
enum class PostMood(val rawValue: String) {
    WARM("따뜻하게"),
    WITTY("재치 있게"),
    CALM("담백하게")
}
