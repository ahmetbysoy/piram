package com.example.domain.engine

import kotlin.math.abs

/**
 * #10 NextCandlePredictionGame — konsensüs gücüne göre "sıradaki 1dk yeşil mi kırmızı mı"
 * tahmini yapar, [windowMs] sonra gerçekleşen fiyatla karşılaştırıp oyunlaştırılmış
 * doğruluk sayacı üretir. Saf, injectable clock, test edilebilir.
 */
class NextCandleGame(
    private val windowMs: Long = 60_000L,
    private val minStrength: Double = 15.0,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class Prediction(val price: Double, val at: Long, val bullish: Boolean)

    private var pending: Prediction? = null
    private var wins = 0
    private var total = 0
    private var streak = 0

    /**
     * Tahmin üretir; bekleyen tahmin varken veya güç yetersizse null.
     * Dönüş: true = yeşil (yükseliş) tahmini.
     */
    @Synchronized
    fun predict(strength: Double, price: Double, at: Long = clock()): Boolean? {
        if (pending != null || !price.isFinite() || price <= 0) return null
        if (abs(strength) < minStrength) return null
        val bullish = strength > 0
        pending = Prediction(price, at, bullish)
        return bullish
    }

    /** Süresi dolan tahmini sonuçlandırır. */
    @Synchronized
    fun resolve(price: Double, now: Long = clock()) {
        val p = pending ?: return
        if (!price.isFinite() || price <= 0) return
        if (now - p.at < windowMs) return
        pending = null
        total++
        val win = if (p.bullish) price >= p.price else price < p.price
        if (win) {
            wins++
            streak++
        } else {
            streak = 0
        }
    }

    @Synchronized
    fun pendingBullish(): Boolean? = pending?.bullish

    @Synchronized
    fun chip(): String {
        val pendingTxt = pending?.let { if (it.bullish) " 🟢 bekliyor" else " 🔴 bekliyor" } ?: ""
        val streakTxt = if (streak >= 2) " 🔥$streak" else ""
        return if (total > 0) "🎯 $wins/$total$streakTxt$pendingTxt" else if (pending != null) "🎯$pendingTxt" else ""
    }

    @Synchronized
    fun clear() {
        pending = null
        wins = 0
        total = 0
        streak = 0
    }
}
