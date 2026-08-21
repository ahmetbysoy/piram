package com.example.domain.engine.strategy

import com.example.domain.model.MarketSnapshot
import com.example.domain.model.OrderSide
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * #1 WhaleFootprintStrategy — son N trade'de whale işlemlerinin ortalama trade'e
 * göre kaç kat büyük olduğuna ve alış/satış dengesine bakar; kurumsal giriş izlerini
 * fiyat üstünde işaretler. `Order.isWhale` zaten MicroBucketManager'dan geliyor.
 */
class WhaleFootprintStrategy : Strategy {
    override val id = "whale_footprint"
    override val name = "Whale Footprint"
    override val description = "Ortalama trade'in katları büyüklüğünde kurumsal giriş izleri"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val trades = data.trades
        if (trades.size < 20) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Mapping whale footprints (${trades.size}/20)")
        }
        val whales = trades.filter { it.isWhale }
        if (whales.isEmpty()) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "No whale prints in buffer")
        }
        val avgAll = trades.map { it.value }.average()
        val whaleAvg = whales.map { it.value }.average()
        val multiple = if (avgAll > 0) whaleAvg / avgAll else 0.0
        val whaleBuy = whales.count { it.side == OrderSide.BUY }
        val whaleSell = whales.count { it.side == OrderSide.SELL }

        val score = when {
            whaleBuy > whaleSell && multiple >= 2.0 -> 0.6
            whaleBuy > whaleSell -> 0.3
            whaleSell > whaleBuy && multiple >= 2.0 -> -0.6
            whaleSell > whaleBuy -> -0.3
            else -> 0.0
        }
        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)
        val reason = "Whale footprints: ${whaleBuy}B/${whaleSell}S (${"%.1f".format(multiple)}x avg)"
        return StrategyResult(
            id, name, signal, SignalThresholds.confidenceFor(score, 0.55, 0.4), score, reason,
            mapOf("multiple" to multiple, "whaleCount" to whales.size.toDouble())
        )
    }
}

/**
 * #4 RoundNumberMagnetStrategy — fiyatın psikolojik yuvarlak seviyelere (100/1000/50000)
 * yakınlığını ölçer; mıknatıs çekimini yön sinyaline çevirir.
 * Fiyat seviyenin üstündeyse aşağı (SELL), altındaysa yukarı (BUY) çekim skoru.
 */
class RoundNumberMagnetStrategy : Strategy {
    override val id = "round_number_magnet"
    override val name = "Round Number Magnet"
    override val description = "Psikolojik yuvarlak seviyelere çekim etkisi"
    override val category = StrategyCategory.TREND

    override fun execute(data: MarketSnapshot): StrategyResult {
        val price = data.currentPrice
        if (price <= 0) return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Waiting for price")
        val round = roundLevel(price)
        val distPct = (price - round) / round * 100.0
        val proximity = abs(distPct)
        if (proximity > 0.5) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Far from round levels")
        }
        val strength = (1.0 - proximity / 0.5).coerceIn(0.0, 1.0)
        val score = (if (distPct > 0) -strength else strength) * 0.6
        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)
        val reason = "Round ${"%.2f".format(round)}: ${"%.3f".format(distPct)}% away"
        return StrategyResult(
            id, name, signal, SignalThresholds.confidenceFor(score, 0.5, 0.45), score, reason,
            mapOf("round" to round, "distPct" to distPct)
        )
    }

    companion object {
        /** Fiyat büyüklüğüne göre en yakın psikolojik seviye. */
        fun roundLevel(price: Double): Double {
            val mag = when {
                price >= 10_000.0 -> 1_000.0
                price >= 1_000.0 -> 100.0
                price >= 100.0 -> 10.0
                price >= 10.0 -> 1.0
                else -> 0.1
            }
            return (price / mag).roundToInt() * mag
        }
    }
}

/**
 * #3 LiquidationCascadeStrategy — 60 sn'lik likidasyon baskısını (adet + notional)
 * fiyat yönüyle birleştirip kademeli çözülme riskini skorlar.
 * `MarketSnapshot.liquidationNotional60s / liquidationCount60s` kullanır.
 */
class LiquidationCascadeStrategy : Strategy {
    override val id = "liquidation_cascade"
    override val name = "Liquidation Cascade"
    override val description = "Ard arda likidasyon baskısı (60sn) ile kademeli çözülme riski"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val notional = data.liquidationNotional60s
        val count = data.liquidationCount60s
        if (count < 3 || notional < 50_000.0) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "No liquidation cascade")
        }
        val prices = data.recentPrices
        val priceDrop = if (prices.size >= 5) {
            val s = prices.takeLast(5)
            if (s.first() > 0) (s.last() - s.first()) / s.first() * 100.0 else 0.0
        } else {
            0.0
        }
        val intensity = (notional / 1_000_000.0).coerceIn(0.0, 2.0)
        val score = when {
            priceDrop < -0.05 -> (-(0.5 + intensity * 0.25)).coerceIn(-1.0, -0.5)
            priceDrop > 0.05 -> (0.3 + intensity * 0.15).coerceIn(0.0, 0.7)
            else -> 0.0
        }
        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)
        val reason = "$count likidasyon / 60sn (${"%.0f".format(notional)} USDT), dprice ${"%.3f".format(priceDrop)}%"
        return StrategyResult(
            id, name, signal, SignalThresholds.confidenceFor(score, 0.55, 0.4), score, reason,
            mapOf("liqCount" to count.toDouble(), "liqNotional" to notional)
        )
    }
}
