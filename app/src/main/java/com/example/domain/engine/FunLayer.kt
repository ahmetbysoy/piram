package com.example.domain.engine

import com.example.domain.model.Depth
import com.example.domain.model.OrderSide
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * #20 MarketPersonality — coin'in son davranışına göre kişilik etiketi.
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
 * Side bilgisi tutar: BUY likidasyon = short'lar zorla kapatıldı, SELL = long'lar.
 */
class LiquidationTracker(
    private val windowMs: Long = 60_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class Sample(val at: Long, val side: OrderSide, val notional: Double)

    private val samples = ArrayDeque<Sample>()

    @Synchronized
    fun record(side: OrderSide, notional: Double, at: Long = clock()) {
        if (!notional.isFinite() || notional <= 0) return
        samples.addLast(Sample(at, side, notional))
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
    fun sumBuy(now: Long = clock()): Double {
        prune(now)
        return samples.filter { it.side == OrderSide.BUY }.sumOf { it.notional }
    }

    @Synchronized
    fun sumSell(now: Long = clock()): Double {
        prune(now)
        return samples.filter { it.side == OrderSide.SELL }.sumOf { it.notional }
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

/**
 * #16 PainScoreNarrator — likidasyon yönüne göre "kim acı çekiyor" anlatısı.
 * SELL likidasyon = long pozisyonlar zorla satılıyor → long'lar acı çekiyor.
 */
object PainScore {

    fun evaluate(sellLiqNotional: Double, buyLiqNotional: Double): String? {
        val diff = sellLiqNotional - buyLiqNotional
        return when {
            sellLiqNotional >= 50_000.0 && diff > buyLiqNotional -> "😖 long'lar acı çekiyor"
            buyLiqNotional >= 50_000.0 && -diff > sellLiqNotional -> "😖 short'lar acı çekiyor"
            else -> null
        }
    }
}

/**
 * #18 CalmBeforeStorm — volatilite sıkışması (kısa stdDev / uzun stdDev düşük) +
 * kitap dengesizliği artıyorsa "fırtına öncesi sessizlik" rozeti.
 */
object CalmBeforeStorm {

    fun evaluate(prices: List<Double>, depth: Depth?): String? {
        if (prices.size < 20 || depth == null) return null
        val short = stdDev(prices.takeLast(6))
        val long = stdDev(prices.takeLast(20))
        if (long <= 0) return null
        val volRatio = short / long
        if (volRatio >= 0.6) return null // sıkışma yok
        val bid = depth.bids.take(5).sumOf { it.volume }
        val ask = depth.asks.take(5).sumOf { it.volume }
        val imbalance = if (bid + ask > 0) (bid - ask) / (bid + ask) else 0.0
        return if (abs(imbalance) > 0.3) "🌪️ Fırtına öncesi sessizlik" else null
    }

    private fun stdDev(values: List<Double>): Double {
        if (values.size <= 1) return 0.0
        val mean = values.sum() / values.size
        val variance = values.map { (it - mean) * (it - mean) }.sum() / (values.size - 1)
        return sqrt(variance)
    }
}

/**
 * #13 PersonalityHistory — kişilik değişimlerinin günlük özeti
 * ("bugün 3 kere ÇILGIN moduna girdi" tarzı).
 */
class PersonalityHistory(
    private val windowMs: Long = 24 * 3_600_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class Entry(val at: Long, val label: String)

    private val entries = ArrayDeque<Entry>()

    @Synchronized
    fun record(label: String, at: Long = clock()) {
        if (label.isEmpty()) return
        entries.addLast(Entry(at, label))
        prune(at)
    }

    @Synchronized
    fun summaryChip(now: Long = clock()): String {
        prune(now)
        if (entries.isEmpty()) return ""
        val counts = entries.groupingBy { it.label }.eachCount()
        val top = counts.maxByOrNull { it.value } ?: return ""
        return "📅 ${top.key}×${top.value}"
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }

    private fun prune(now: Long) {
        val cutoff = now - windowMs
        while (entries.isNotEmpty() && entries.first().at < cutoff) {
            entries.removeFirst()
        }
    }
}
