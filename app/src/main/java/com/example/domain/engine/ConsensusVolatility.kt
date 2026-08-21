package com.example.domain.engine

import kotlin.math.sqrt

/**
 * #27 consensusVolatilityBand — son N konsensüs gücünün stdDev'i.
 * Yüksek stdDev = konsensüs bir BUY bir SELL arasında gidip geliyor (kararsız).
 */
object ConsensusVolatility {

    data class Band(val stdDev: Double, val n: Int) {
        val isUnstable: Boolean get() = n >= 5 && stdDev > 25.0
    }

    fun band(history: List<Double>): Band {
        if (history.size < 2) return Band(0.0, history.size)
        val mean = history.sum() / history.size
        val variance = history.map { (it - mean) * (it - mean) }.sum() / history.size
        return Band(sqrt(variance), history.size)
    }
}
