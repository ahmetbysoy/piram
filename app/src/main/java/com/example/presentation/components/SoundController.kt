package com.example.presentation.components

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * #12 WhaleAlertSound — sistem tonu tabanlı ses efektleri (ses dosyası gerektirmez).
 * Whale = çift yüksek bip, Burst = kısa tek bip. Sessiz modda otomatik saygılıdır
 * (ToneGenerator akışını ses yönetimi belirler).
 */
class SoundController {

    private fun play(tone: Int, durationMs: Int) {
        val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
        try {
            generator.startTone(tone, durationMs)
        } catch (_: Exception) {
            generator.release()
        }
        // startTone asenkron; kısa gecikme sonrası serbest bırak
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            generator.release()
        }, durationMs + 100L)
    }

    fun playWhale(enabled: Boolean = true) {
        if (!enabled) return
        play(ToneGenerator.TONE_PROP_BEEP2, 120)
    }

    fun playBurst(enabled: Boolean = true) {
        if (!enabled) return
        play(ToneGenerator.TONE_PROP_BEEP, 60)
    }
}
