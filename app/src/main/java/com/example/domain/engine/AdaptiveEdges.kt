package com.example.domain.engine

/**
 * Adaptif eşik (piramit'ten port): coin'in kendi notional dağılımından katman aralığı.
 * Histerezis ViewModel tarafında uygulanır (SignalConfig.HYSTERESIS).
 */
object AdaptiveEdges {

    /** Sıralı listeden yüzdelik değeri. */
    fun percentile(sortedValues: List<Double>, p: Double): Double {
        if (sortedValues.isEmpty()) return 0.0
        val idx = ((sortedValues.size - 1) * p).toInt().coerceIn(0, sortedValues.size - 1)
        return sortedValues[idx]
    }

    /**
     * Son N trade'in notional dağılımından [alt, üst] eşik aralığı.
     * Yeterli örnek yoksa null döner (sabit aralık korunur).
     */
    fun adaptiveRange(notionals: List<Double>): Pair<Double, Double>? {
        if (notionals.size < SignalConfig.ADAPT_MIN_TRADES) return null
        val sorted = notionals.sorted()
        val lo = percentile(sorted, 0.05).coerceAtLeast(SignalConfig.MIN_NOTIONAL)
        val hi = percentile(sorted, 0.995).coerceAtLeast(lo * 2)
        return lo to hi
    }
}
