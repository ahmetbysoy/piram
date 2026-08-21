package com.example.domain.engine

import kotlin.math.abs

/**
 * #20 MarketPersonality — coin'in son davranışına göre kişilik etiketi.
 * Saf Kotlin, test edilebilir.
 */
object MarketPersonality {

    fun evaluate(whalePct: Double, burstCount: Int, changePct: Double, ofi: Double): Pair<String, String> {
        val absChange = abs(changePct)
        return when {
            whalePct >= 60.0 && burstCount >= 2 -> "AGRESİF" to "🦈"
            whalePct >= 60.0 && absChange < 0.10 -> "SİNSİ" to "🐙"
            burstCount >= 3 -> "ÇILGIN" to "🔥"
            absChange < 0.03 && whalePct < 40.0 -> "SAKİN" to "😴"
            else -> "KARARSIZ" to "🎲"
        }
    }
}

/**
 * #17 RektMeter — 60 sn'lik likidasyon notional'ına göre "rekt" seviyesi (0-5).
 */
object RektMeter {

    fun level(notional60s: Double): Int = when {
        notional60s >= 10_000_000.0 -> 5
        notional60s >= 5_000_000.0 -> 4
        notional60s >= 1_000_000.0 -> 3
        notional60s >= 250_000.0 -> 2
        notional60s >= 50_000.0 -> 1
        else -> 0
    }

    fun emoji(level: Int): String = when (level) {
        5 -> "🔥🔥🔥"
        4 -> "🔥🔥"
        in 1..3 -> "🔥"
        else -> ""
    }

    fun label(level: Int): String = when (level) {
        5 -> "REKT"
        4 -> "KIYIM"
        3 -> "SIKINTI"
        else -> ""
    }
}

/**
 * #19 WhaleRetailBoard — kurumsal vs perakende skor tablosu verisi.
 */
data class WhaleRetailBoardState(
    val whalePct: Double,   // 0..1
    val winner: String,
    val score: String
)

object WhaleRetailBoard {

    fun evaluate(whaleNotional: Double, retailNotional: Double): WhaleRetailBoardState? {
        val total = whaleNotional + retailNotional
        if (total <= 0) return null
        val whalePct = whaleNotional / total
        val winner = when {
            whalePct > 0.5 -> "🐋 BALİNA"
            whalePct < 0.5 -> "🐟 PERAKENDE"
            else -> "⚖️ DENGEDE"
        }
        val score = when {
            whalePct > 0.7 -> "3-0"
            whalePct > 0.5 -> "2-1"
            whalePct < 0.3 -> "0-3"
            whalePct < 0.5 -> "1-2"
            else -> "1-1"
        }
        return WhaleRetailBoardState(whalePct, winner, score)
    }
}

/**
 * #17 destek: 60 sn'lik likidasyon notional penceresi (pure, injectable clock).
 */
class LiquidationTracker(
    private val windowMs: Long = 60_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class Sample(val at: Long, val notional: Double)

    private val samples = ArrayDeque<Sample>()

    @Synchronized
    fun record(notional: Double, at: Long = clock()) {
        if (!notional.isFinite() || notional <= 0) return
        samples.addLast(Sample(at, notional))
        prune(at)
    }

    @Synchronized
    fun sum(now: Long = clock()): Double {
        prune(now)
        return samples.sumOf { it.notional }
    }

    @Synchronized
    fun count(now: Long = clock()): Int {
        prune(now)
        return samples.size
    }

    @Synchronized
    fun clear() {
        samples.clear()
    }

    private fun prune(now: Long) {
        val cutoff = now - windowMs
        while (samples.isNotEmpty() && samples.first().at < cutoff) {
            samples.removeFirst()
        }
    }
}
