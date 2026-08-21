package com.example.domain.engine

import com.example.domain.model.SignalType

/**
 * #7 MultiTimeframeConsensus — aynı strateji setinin kısa (60sn) ve uzun (oturum)
 * penceredeki konsensüsünü karşılaştırıp çapraz zaman uyarısı üretir.
 * Saf Kotlin, test edilebilir.
 */
object MultiTimeframeConsensus {

    private fun direction(s: SignalType): Int = when (s) {
        SignalType.STRONG_BUY, SignalType.BUY -> 1
        SignalType.STRONG_SELL, SignalType.SELL -> -1
        SignalType.NEUTRAL -> 0
    }

    /**
     * İki yön de net ise Türkçe çapraz-zaman cümlesi; aksi halde null.
     */
    fun compare(shortSignal: SignalType, sessionSignal: SignalType): String? {
        val shortDir = direction(shortSignal)
        val sessDir = direction(sessionSignal)
        if (shortDir == 0 || sessDir == 0) return null
        return when {
            shortDir == sessDir && shortDir > 0 -> "⏱ Kısa ve uzun aynı: ALIŞ teyitli"
            shortDir == sessDir -> "⏱ Kısa ve uzun aynı: SATIŞ teyitli"
            shortDir < 0 -> "⏱ 1dk SATIŞ, açılıştan ALIŞ — karışık zaman"
            else -> "⏱ 1dk ALIŞ, açılıştan SATIŞ — dip toplama olabilir"
        }
    }
}
