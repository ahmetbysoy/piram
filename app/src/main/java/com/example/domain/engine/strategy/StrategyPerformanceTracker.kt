package com.example.domain.engine.strategy

import kotlin.math.abs

/** UI için strateji performans özeti. */
data class StrategyPerfStats(
    val strategyId: String,
    val name: String,
    val resolved: Int,
    val hits: Int
) {
    val winRate: Double get() = if (resolved > 0) hits.toDouble() / resolved else 0.0
}

/**
 * Strateji performans izleyicisi (#21): her stratejinin yön sinyalini kaydeder,
 * [windowMs] sonra fiyatla karşılaştırıp isabet/ıska sayar ve `executeAll` için
 * dinamik ağırlık üretir. Kötü performans gösteren strateji otomatik zayıflar.
 *
 * - Yalnızca yönlü (BUY/SELL) ve |score| ≥ [MIN_SCORE] sinyaller kaydedilir.
 * - Strateji başına [recordIntervalMs] içinde tek kayıt (throttle) — çift çağrı
 *   (pyramid loop + strategies screen) yine de tek örnek üretir.
 * - [MIN_SAMPLES] altında ağırlık nötr (1.0) — soğuk başlangıçta ceza yok.
 * - Saf Kotlin; clock inject edilebilir, JUnit ile test edilebilir.
 */
class StrategyPerformanceTracker(
    private val windowMs: Long = 60_000L,
    private val recordIntervalMs: Long = 5_000L,
    private val minScore: Double = 0.15,
    private val minSamples: Int = 10,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private class Prediction(
        val id: String,
        val price: Double,
        val at: Long,
        val bullish: Boolean
    )

    private val pending = ArrayDeque<Prediction>()
    private val resolved = HashMap<String, Int>()
    private val hits = HashMap<String, Int>()
    private val lastRecord = HashMap<String, Long>()

    /** Yönlü bir sinyali kaydeder (throttle'lı). */
    @Synchronized
    fun record(id: String, bullish: Boolean, price: Double, at: Long = clock()) {
        if (!price.isFinite() || price <= 0) return
        val last = lastRecord[id]
        if (last != null && at - last < recordIntervalMs) return
        lastRecord[id] = at
        pending.addLast(Prediction(id, price, at, bullish))
    }

    /** Süresi dolan tahminleri o anki fiyatla sonuçlandırır. */
    @Synchronized
    fun resolve(price: Double, now: Long = clock()) {
        if (!price.isFinite()) return
        while (pending.isNotEmpty() && now - pending.first().at >= windowMs) {
            val p = pending.removeFirst()
            val hit = if (p.bullish) price >= p.price else price < p.price
            resolved[p.id] = (resolved[p.id] ?: 0) + 1
            if (hit) hits[p.id] = (hits[p.id] ?: 0) + 1
        }
    }

    fun resolvedCount(id: String): Int = resolved[id] ?: 0

    fun hitCount(id: String): Int = hits[id] ?: 0

    fun winRate(id: String): Double {
        val r = resolved[id] ?: 0
        if (r == 0) return 0.0
        return (hits[id] ?: 0).toDouble() / r
    }

    /**
     * Adaptif ağırlık: 0.5 + winRate, [0.3 .. 1.2] aralığına kırpılır.
     * Örnek: winRate %50 → 1.0 (nötr), %0 → 0.5 (yarı), %100 → 1.2 (tavan).
     * Soğuk başlangıçta 1.0.
     */
    fun weight(id: String): Double {
        val r = resolved[id] ?: 0
        if (r < minSamples) return 1.0
        val rate = (hits[id] ?: 0).toDouble() / r
        return (0.5 + rate).coerceIn(0.3, 1.2)
    }

    fun clear() {
        pending.clear()
        resolved.clear()
        hits.clear()
        lastRecord.clear()
    }

    /** Kayıt eşiğinin üstünde mi? (yönlü + yeterli skor) — çağıran kolaylık için. */
    fun shouldRecord(score: Double): Boolean = abs(score) >= minScore
}
