package com.example.domain.engine.strategy

import com.example.domain.model.SignalType
import kotlin.math.abs

/**
 * Ortak sinyal eşikleri + güven formülü (DRY).
 * 20 stratejide kopyalanmış `when` blokları ve `base + |score|*k` güven
 * hesaplarını tek noktada toplar. Davranış korunur; parametreler stratejiye özeldir.
 */
object SignalThresholds {

    /**
     * Skoru simetrik eşiklerle sinyale çevirir.
     * `score > strong` → STRONG_BUY, `> weak` → BUY, simetriği SELL, aksi NEUTRAL.
     */
    fun signalFor(score: Double, strong: Double = 0.45, weak: Double = 0.15): SignalType = when {
        score > strong -> SignalType.STRONG_BUY
        score > weak -> SignalType.BUY
        score < -strong -> SignalType.STRONG_SELL
        score < -weak -> SignalType.SELL
        else -> SignalType.NEUTRAL
    }

    /** Güven: `(base + |score| * scale)` → [0..1]. */
    fun confidenceFor(score: Double, base: Double = 0.55, scale: Double = 0.4): Double =
        (base + abs(score) * scale).coerceIn(0.0, 1.0)
}
