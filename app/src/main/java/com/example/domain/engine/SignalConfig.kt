package com.example.domain.engine

/**
 * Merkezi sinyal / motor eşikleri. Sihirli sayılar tek yerde; tune buradan.
 *
 * Notional (USDT) esaslı katmanlama: `Order.value = price × volume` her zaman USDT'dir.
 */
object SignalConfig {

    // --- Notional katmanlama (USDT) ---
    const val DEFAULT_LAYERS = 8
    const val MIN_NOTIONAL = 100.0          // alt sınır: 100 USDT
    const val MAX_NOTIONAL = 1_000_000.0    // üst sınır: 1M USDT

    // BTC ölçeği sabit tablo (piramit'ten — 7 katman; referans olarak tutulur)
    val FIXED_EDGES = doubleArrayOf(
        0.0, 100.0, 1_000.0, 10_000.0, 50_000.0, 250_000.0, 1_000_000.0, Double.POSITIVE_INFINITY
    )
    const val BTC_MEDIAN_REF = 4000.0       // BTC referans medyan notional (USDT)

    // --- Adaptif eşik ---
    const val ADAPT_MIN_TRADES = 40
    val ADAPT_PERCENTILES = doubleArrayOf(0.5, 0.75, 0.9, 0.97, 0.99, 0.999)
    const val HYSTERESIS = 0.18             // eşik zıplamasın

    // --- Sönüm / yumuşatma / motor döngüsü ---
    const val DEFAULT_DECAY = 0.15f
    const val DISPLAY_SMOOTHING = 0.22f
    const val ENGINE_TICK_MS = 80L          // ~12fps state loop
    const val STRATEGY_RUN_MS = 250L
    const val MIN_PRICES_FOR_STRATEGY = 5

    // --- Divergence (toplama / boşaltma) ---
    const val PX_TAU = 0.85
    const val DIV_MIN_NOTIONAL = 500.0      // altındaysa sinyal üretme
    const val DIV_SCORE = 0.12
    const val CLASH_MIN = 800.0

    /** BTC referans medyanına göre sabit eşik ölçekleme katsayısı (0.02 .. 2.5). */
    fun sizeScale(medianNotional: Double): Double {
        if (!medianNotional.isFinite() || medianNotional <= 0) return 1.0
        val s = medianNotional / BTC_MEDIAN_REF
        return s.coerceIn(0.02, 2.5)
    }
}
