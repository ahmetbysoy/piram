package com.example.domain.engine.strategy

import com.example.domain.model.MarketSnapshot
import com.example.domain.model.OrderSide
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import kotlin.math.abs

/**
 * 16. StatisticalArbitrageStrategy
 * Multi-exchange price divergence & spread arbitrage opportunities.
 */
class StatisticalArbitrageStrategy : Strategy {
    override val id = "statistical_arbitrage"
    override val name = "Statistical Arbitrage"
    override val description = "Multi-exchange cross-venue price spread & dispersion Z-score"
    override val category = StrategyCategory.ARBITRAGE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.exchangePrices.values.toList()
        if (prices.size < 2) {
            // Single exchange fallback
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Tracking venue prices (${prices.size} venue active)")
        }

        val minPrice = prices.minOrNull() ?: data.currentPrice
        val maxPrice = prices.maxOrNull() ?: data.currentPrice
        val mean = prices.sum() / prices.size
        val spreadBps = if (mean > 0) ((maxPrice - minPrice) / mean) * 10000.0 else 0.0
        val priceDiff = (data.currentPrice - mean) / mean

        val score = when {
            spreadBps > 15.0 && priceDiff < 0 -> 0.75 // This exchange lagging behind others -> Long Arb
            spreadBps > 15.0 && priceDiff > 0 -> -0.75 // This exchange leading ahead -> Short Arb
            else -> 0.0
        }

        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)

        val reason = "Cross-venue Spread: ${"%.1f".format(spreadBps)} bps across ${prices.size} exchanges"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, base = 0.55, scale = 0.4), score, reason, mapOf("spreadBps" to spreadBps, "venues" to prices.size.toDouble()))
    }
}

/**
 * 17. TimeBasedMomentumStrategy
 * Inter-trade arrival time delta and order frequency acceleration.
 */
class TimeBasedMomentumStrategy : Strategy {
    override val id = "time_based_momentum"
    override val name = "Trade Arrival Velocity"
    override val description = "Microsecond inter-trade arrival rate acceleration & clustering"
    override val category = StrategyCategory.MOMENTUM

    override fun execute(data: MarketSnapshot): StrategyResult {
        val trades = data.trades
        if (trades.size < 12) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Timing inter-arrival intervals")
        }

        val recent10 = trades.takeLast(10)
        val timeSpan = (recent10.last().timestamp - recent10.first().timestamp).coerceAtLeast(10L)
        val tradesPerSec = (recent10.size.toDouble() / (timeSpan.toDouble() / 1000.0)).coerceIn(0.1, 500.0)

        val buyCount = recent10.count { it.side == OrderSide.BUY }
        val buyDominance = (buyCount - 5) / 5.0 // -1 to +1

        val isHighFrequency = tradesPerSec > 15.0
        val score = if (isHighFrequency) (buyDominance * 1.1).coerceIn(-1.0, 1.0) else buyDominance * 0.4

        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)

        val reason = "Arrival Velocity: ${"%.1f".format(tradesPerSec)} trades/sec [${if (isHighFrequency) "HFT BURST" else "STEADY"}]"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, base = 0.5, scale = 0.45), score, reason, mapOf("tradesPerSec" to tradesPerSec))
    }
}

/**
 * 18. OrderBookPressureStrategy
 * Multi-level L2 order book weighted slope imbalance (levels 1-10).
 */
class OrderBookPressureStrategy : Strategy {
    override val id = "order_book_pressure"
    override val name = "Order Book Pressure"
    override val description = "10-depth level weighted bid/ask queue gradient pressure"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val depth = data.depth
        if (depth == null || depth.bids.isEmpty() || depth.asks.isEmpty()) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Calculating book gradient")
        }

        var weightedBid = 0.0
        var weightedAsk = 0.0

        depth.bids.take(10).forEachIndexed { i, level ->
            val weight = 1.0 / (i + 1.0)
            weightedBid += level.volume * weight
        }

        depth.asks.take(10).forEachIndexed { i, level ->
            val weight = 1.0 / (i + 1.0)
            weightedAsk += level.volume * weight
        }

        val total = weightedBid + weightedAsk
        val pressure = if (total > 0) (weightedBid - weightedAsk) / total else 0.0

        val score = (pressure * 1.2).coerceIn(-1.0, 1.0)
        val signal = SignalThresholds.signalFor(score, strong = 0.45, weak = 0.15)

        val reason = "Bid Pressure: ${"%.1f".format(weightedBid)}, Ask Pressure: ${"%.1f".format(weightedAsk)}"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, base = 0.6, scale = 0.35), score, reason, mapOf("pressure" to pressure))
    }
}

/**
 * 19. PriceActionStrategy
 * Microstructure tick absorption, pin-bars, and immediate continuation patterns.
 */
class PriceActionStrategy : Strategy {
    override val id = "price_action"
    override val name = "Tick Price Action"
    override val description = "Micro-range absorption pins & immediate directional rejection"
    override val category = StrategyCategory.TREND

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 10) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Evaluating tick candles")
        }

        val current = data.currentPrice
        val recentSample = prices.takeLast(10)
        val maxP = recentSample.maxOrNull() ?: current
        val minP = recentSample.minOrNull() ?: current
        val range = maxP - minP

        if (range <= 0.00001) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Zero price variance")
        }

        val closeLocation = (current - minP) / range // 0.0 = bottom wick, 1.0 = top
        val score = ((closeLocation - 0.5) * 1.8).coerceIn(-1.0, 1.0)

        val signal = SignalThresholds.signalFor(score, strong = 0.55, weak = 0.15)

        val reason = "Range: ${"%.2f".format(range)}, Relative Close: ${"%.1f".format(closeLocation * 100)}%"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, base = 0.5, scale = 0.45), score, reason, mapOf("closeLoc" to closeLocation))
    }
}

/**
 * 20. BurstArbitrageStrategy
 * Real-time burst cluster follow-through momentum and exhaustion detection.
 */
class BurstArbitrageStrategy : Strategy {
    override val id = "burst_arbitrage"
    override val name = "Burst Momentum Arb"
    override val description = "Directional burst cluster surge follow-through & exhaustion"
    override val category = StrategyCategory.ARBITRAGE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val bursts = data.bursts
        if (bursts.isEmpty()) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "No active burst cluster")
        }

        val latestBurst = bursts.last()
        val intensity = latestBurst.intensityScore
        val isBuy = latestBurst.side == OrderSide.BUY

        // High intensity burst creates immediate directional thrust
        val score = if (isBuy) {
            (intensity / 10.0).coerceIn(0.3, 0.95)
        } else {
            -(intensity / 10.0).coerceIn(0.3, 0.95)
        }

        val signal = SignalThresholds.signalFor(score, strong = 0.6, weak = 0.2)

        val reason = "Active ${latestBurst.side} Burst! Orders: ${latestBurst.orderCount}, Intensity: ${"%.1f".format(intensity)}"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, base = 0.65, scale = 0.3), score, reason, mapOf("burstIntensity" to intensity, "burstVol" to latestBurst.totalVolume))
    }
}
