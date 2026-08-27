package com.armsone.starmanager.ui.externalai

import java.util.Locale

/**
 * 외부 AI 생성 대기 경과 시간 포맷터.
 *
 * iOS 요구사항:
 * - 0초~59초: "답변을 기다리는 중 (00초)" ~ "답변을 기다리는 중 (59초)"
 * - 60초 이상: "답변을 기다리는 중 (1분 00초)" ~ "답변을 기다리는 중 (1분 05초)"
 */
object ExternalAITimerFormatter {

    fun formatElapsedSeconds(seconds: Long): String {
        val clamped = seconds.coerceAtLeast(0L)
        val minutes = clamped / 60
        val remSec = clamped % 60
        return if (minutes > 0) {
            String.format(Locale.ROOT, "%d분 %02d초", minutes, remSec)
        } else {
            String.format(Locale.ROOT, "%02d초", remSec)
        }
    }

    fun formatWaitingStatus(seconds: Long): String {
        return "답변을 기다리는 중 (${formatElapsedSeconds(seconds)})"
    }
}
