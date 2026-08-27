package com.armsone.starmanager.ui.externalai

object ExternalAIAnswerCleaner {

    private val thinkTagRegex = Regex("(?is)<think>.*?</think>")
    private val codeFenceRegex = Regex("(?s)^```(?:[a-zA-Z0-9_-]+)?\\s*\\n?(.*?)\\n?```$")
    private val preambleRegex = Regex(
        "(?im)^(?:네[,\\.]?\\s*)?(?:다음은\\s+.*?(?:입니다|글입니다|본문입니다|내용입니다)[:\\.]?|" +
            "요청하신\\s+.*?(?:입니다|드립니다|내용입니다)[:\\.]?|" +
            "인스타그램\\s+(?:게시물|본문|문구).*?(?:입니다|드립니다)[:\\.]?|" +
            "여기\\s+.*?(?:있습니다|드립니다)[:\\.]?)\\s*\\n+"
    )

    fun clean(raw: String): String {
        var text = raw.trim()
        if (text.isEmpty()) return ""

        // 1. <think>...</think> 태그 제거
        text = thinkTagRegex.replace(text, "").trim()

        // 2. 전체 코드 펜스(```...```) 제거
        val fenceMatch = codeFenceRegex.find(text)
        if (fenceMatch != null) {
            text = fenceMatch.groupValues[1].trim()
        }

        // 3. 서두 대화형 안내 문구 제거
        text = preambleRegex.replace(text, "").trim()

        // 4. 프롬프트 헤더([내가 입력한 내용], [원하는 결과])가 답변 앞단에 복제된 경우 정리
        if (text.contains("[원하는 결과]")) {
            val parts = text.split("[원하는 결과]")
            val after = parts.lastOrNull()?.trim() ?: ""
            // 결과 블록 이후에서 실제 해시태그나 본문 시작 부분을 찾음
            val lines = after.lines()
            val contentLines = lines.dropWhile { line ->
                val l = line.trim()
                l.startsWith("-") || l.startsWith("다른 설명") || l.isEmpty()
            }
            if (contentLines.isNotEmpty()) {
                text = contentLines.joinToString("\n").trim()
            }
        } else if (text.startsWith("[내가 입력한 내용]")) {
            val lines = text.lines()
            val contentLines = lines.dropWhile { line ->
                val l = line.trim()
                l.startsWith("[") || l.startsWith("-") || l.isEmpty()
            }
            if (contentLines.isNotEmpty()) {
                text = contentLines.joinToString("\n").trim()
            }
        }

        // 5. 전체를 감싸는 따옴표 제거
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
            (text.startsWith("“") && text.endsWith("”"))
        ) {
            text = text.substring(1, text.length - 1).trim()
        }

        return text
    }
}
