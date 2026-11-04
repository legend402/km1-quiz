package com.km1quiz.app.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/** 8-bit 风音效（ToneGenerator，零依赖；可在首页静音） */
object Sfx {

    @Volatile
    var enabled: Boolean = true

    private var tone: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun ensure(): ToneGenerator? = try {
        tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 70).also { tone = it }
    } catch (_: RuntimeException) {
        null // 音频资源被占等场景：静默失败，不影响使用
    }

    private fun play(toneId: Int, durationMs: Int) {
        if (!enabled) { return }
        val t = ensure() ?: return
        try { t.startTone(toneId, durationMs) } catch (_: Exception) { /* 忽略 */ }
    }

    fun tap() = play(ToneGenerator.TONE_PROP_BEEP, 36)

    fun correct() = play(ToneGenerator.TONE_PROP_ACK, 120)

    fun wrong() = play(ToneGenerator.TONE_PROP_NACK, 180)

    /** 及格小旋律：三个上行短音 */
    fun fanfare() {
        if (!enabled) { return }
        play(ToneGenerator.TONE_PROP_BEEP2, 70)
        handler.postDelayed({ play(ToneGenerator.TONE_PROP_BEEP, 70) }, 140)
        handler.postDelayed({ play(ToneGenerator.TONE_PROP_ACK, 180) }, 300)
    }

    fun release() {
        tone?.release()
        tone = null
    }
}
