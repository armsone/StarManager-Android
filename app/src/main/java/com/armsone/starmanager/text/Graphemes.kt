package com.armsone.starmanager.text

import java.text.Normalizer

/**
 * Swift의 Character(확장 문자소 클러스터) 단위 문자열 연산을 재현한다.
 * Android 기기에서는 ICU(android.icu.text.BreakIterator, minSdk 26 보장)를 쓰고,
 * JVM 단위 테스트처럼 android.icu가 없는 환경에서는 java.util.regex의 \X
 * (확장 문자소 클러스터, JDK 9+)와 근사 이모지 범위로 대체한다.
 */
object Graphemes {

    interface Engine {
        fun clusters(text: String): List<String>
        fun isEmojiPresentation(codePoint: Int): Boolean
        fun isEmoji(codePoint: Int): Boolean
    }

    @Volatile
    var engine: Engine = createDefaultEngine()

    private fun createDefaultEngine(): Engine = try {
        IcuEngine().also { it.clusters("가") }
    } catch (_: Throwable) {
        PortableEngine()
    }

    fun clusters(text: String): List<String> = engine.clusters(text)

    /** Swift String.count — Character(문자소) 개수. */
    fun count(text: String): Int = if (text.isEmpty()) 0 else clusters(text).size

    /** Swift String.prefix(n) — 문자소 단위 접두어. */
    fun prefix(text: String, n: Int): String {
        if (n <= 0) return ""
        val all = clusters(text)
        if (all.size <= n) return text
        return all.subList(0, n).joinToString("")
    }

    /** Swift character.unicodeScalars 검사와 동일한 이모지 판별. */
    fun isEmojiCluster(cluster: String): Boolean =
        cluster.codePoints().anyMatch { cp ->
            engine.isEmojiPresentation(cp) || (engine.isEmoji(cp) && cp >= 0x1F000)
        }

    /** 모든 스칼라가 한글 완성형 음절(가..힣)인지 — Swift isHangul과 동일. */
    fun isHangulCluster(cluster: String): Boolean =
        cluster.isNotEmpty() && cluster.codePoints().allMatch { it in 0xAC00..0xD7A3 }

    fun containsEmoji(text: String): Boolean = clusters(text).any { isEmojiCluster(it) }

    /** Swift precomposedStringWithCanonicalMapping */
    fun nfc(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC)

    private class IcuEngine : Engine {
        private val legacyEmoji = PortableEngine()

        override fun clusters(text: String): List<String> {
            if (text.isEmpty()) return emptyList()
            val iterator = android.icu.text.BreakIterator.getCharacterInstance()
            iterator.setText(text)
            val result = ArrayList<String>()
            var start = iterator.first()
            var end = iterator.next()
            while (end != android.icu.text.BreakIterator.DONE) {
                result.add(text.substring(start, end))
                start = end
                end = iterator.next()
            }
            return result
        }

        @android.annotation.SuppressLint("InlinedApi")
        override fun isEmojiPresentation(codePoint: Int): Boolean =
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                android.icu.lang.UCharacter.hasBinaryProperty(
                    codePoint, android.icu.lang.UProperty.EMOJI_PRESENTATION
                )
            } else {
                legacyEmoji.isEmojiPresentation(codePoint)
            }

        @android.annotation.SuppressLint("InlinedApi")
        override fun isEmoji(codePoint: Int): Boolean =
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                android.icu.lang.UCharacter.hasBinaryProperty(
                    codePoint, android.icu.lang.UProperty.EMOJI
                )
            } else {
                legacyEmoji.isEmoji(codePoint)
            }
    }

    /** ICU가 없는 JVM 테스트 전용 근사 구현. */
    private class PortableEngine : Engine {
        private val clusterRegex = Regex("\\X")

        override fun clusters(text: String): List<String> =
            clusterRegex.findAll(text).map { it.value }.toList()

        override fun isEmojiPresentation(codePoint: Int): Boolean =
            codePoint in EMOJI_PRESENTATION_BMP || codePoint >= 0x1F300 && codePoint <= 0x1FAFF

        override fun isEmoji(codePoint: Int): Boolean =
            isEmojiPresentation(codePoint) || codePoint in 0x1F000..0x1FAFF

        companion object {
            // 앱 문자열에 실제 등장하는 BMP 이모지(✨ 등)를 포함한 대표 집합.
            private val EMOJI_PRESENTATION_BMP: Set<Int> = buildSet {
                addAll(0x231A..0x231B)
                addAll(0x23E9..0x23EC)
                add(0x23F0); add(0x23F3)
                addAll(0x25FD..0x25FE)
                addAll(0x2614..0x2615)
                addAll(0x2648..0x2653)
                add(0x267F); add(0x2693); add(0x26A1)
                addAll(0x26AA..0x26AB)
                addAll(0x26BD..0x26BE)
                addAll(0x26C4..0x26C5)
                add(0x26CE); add(0x26D4); add(0x26EA)
                addAll(0x26F2..0x26F3)
                add(0x26F5); add(0x26FA); add(0x26FD)
                add(0x2705)
                addAll(0x270A..0x270B)
                add(0x2728); add(0x274C); add(0x274E)
                addAll(0x2753..0x2755)
                add(0x2757)
                addAll(0x2795..0x2797)
                add(0x27B0); add(0x27BF)
                addAll(0x2B1B..0x2B1C)
                add(0x2B50); add(0x2B55)
            }
        }
    }
}
