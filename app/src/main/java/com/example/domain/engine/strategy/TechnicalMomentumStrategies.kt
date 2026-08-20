package com.example.domain.engine.strategy

import com.example.domain.model.MarketSnapshot
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import kotlin.math.abs

/**
 * 6. MacdStrategy
 * 12/26/9 MACD line, signal line, and histogram momentum divergence.
 */
class MacdStrategy : Strategy {
    override val id = "macd"
    override val name = "MACD Histogram"
    override val description = "Moving Average Convergence Divergence with signal cross"
    override val category = StrategyCategory.MOMENTUM

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 28) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Need min 28 ticks for MACD")
        }

        val (macdLine, signalLine, histogram) = TechnicalIndicators.macd(prices, 12, 26, 9)
        val score = (histogram / (data.currentPrice * 0.001)).coerceIn(-1.0, 1.0)

        val signal = when {
            score > 0.45 && macdLine > signalLine -> SignalType.STRONG_BUY
            score > 0.10 -> SignalType.BUY
            score < -0.45 && macdLine < signalLine -> SignalType.STRONG_SELL
            score < -0.10 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "MACD: ${"%.3f".format(macdLine)}, Signal: ${"%.3f".format(signalLine)}, Hist: ${"%.3f".format(histogram)}"
        return StrategyResult(id, name, signal, (0.55 + abs(score) * 0.4).coerceIn(0.0, 1.0), score, reason, mapOf("macd" to macdLine, "signal" to signalLine, "hist" to histogram))
    }
}

/**
 * 7. BollingerBandsStrategy
 * Volatility band squeeze, expansion, and breakout detection.
 */
class BollingerBandsStrategy : Strategy {
    override val id = "bollinger_bands"
    override val name = "Bollinger Bands"
    override val description = "20-period 2-stdDev volatility bands with squeeze analysis"
    override val category = StrategyCategory.VOLATILITY

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 20) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Building Bollinger sample")
        }

        val (upper, middle, lower) = TechnicalIndicators.bollingerBands(prices, 20, 2.0)
        val currentPrice = data.currentPrice
        val width = upper - lower
        val bandwidthPct = if (middle > 0) (width / middle) * 100.0 else 0.0

        val score = when {
            currentPrice >= upper -> 0.70 // Trend ride breakout
            currentPrice <= lower -> -0.70 // Breakdown
            currentPrice > middle -> ((currentPrice - middle) / (upper - middle)) * 0.5
            else -> -((middle - currentPrice) / (middle - lower)) * 0.5
        }

        val signal = when {
            score > 0.5 -> SignalType.STRONG_BUY
            score > 0.15 -> SignalType.BUY
            score < -0.5 -> SignalType.STRONG_SELL
            score < -0.15 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "Upper: ${"%.2f".format(upper)}, Lower: ${"%.2f".format(lower)}, BW: ${"%.2f".format(bandwidthPct)}%"
        return StrategyResult(id, name, signal, (0.5 + abs(score) * 0.45).coerceIn(0.0, 1.0), score, reason, mapOf("upper" to upper, "lower" to lower, "bandwidth" to bandwidthPct))
    }
}

/**
 * 8. SupportResistanceStrategy
 * Dynamic volume-weighted price clusters and pivot levels.
 */
class SupportResistanceStrategy : Strategy {
    override val id = "support_resistance"
    override val name = "Support & Resistance"
    override val description = "Dynamic volume-weighted pivot levels & breakout tests"
    override val category = StrategyCategory.TREND

    override fun execute(data: MarketSnapshot): StrategyResult {
        val trades = data.trades
        if (trades.size < 15) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Analyzing volume pivots")
        }

        val currentPrice = data.currentPrice
        val high = trades.maxOf { it.price }
        val low = trades.minOf { it.price }
        val range = high - low

        if (range <= 0) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Flat price action")
        }

        val distToHigh = (high - currentPrice) / range
        val distToLow = (currentPrice - low) / range

        val score = when {
            distToLow < 0.15 && data.orderFlowImbalance > 0.2 -> 0.65 // Bounce off support with buy flow
            distToHigh < 0.15 && data.orderFlowImbalance < -0.2 -> -0.65 // Rejection from resistance with sell flow
            distToLow < 0.15 && data.orderFlowImbalance < -0.3 -> -0.75 // Support breakdown!
            distToHigh < 0.15 && data.orderFlowImbalance > 0.3 -> 0.75 // Resistance breakout!
            else -> (distToLow - distToHigh) * 0.4
        }

        val signal = when {
            score > 0.5 -> SignalType.STRONG_BUY
            score > 0.15 -> SignalType.BUY
            score < -0.5 -> SignalType.STRONG_SELL
            score < -0.15 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "Res: ${"%.2f".format(high)}, Supp: ${"%.2f".format(low)}, Pos: ${"%.1f".format(distToLow * 100)}%"
        return StrategyResult(id, name, signal, (0.5 + abs(score) * 0.45).coerceIn(0.0, 1.0), score, reason, mapOf("high" to high, "low" to low))
    }
}

/**
 * 9. BreakoutStrategy
 * Donchian 20-channel high/low penetration with volume surge.
 */
class BreakoutStrategy : Strategy {
    override val id = "breakout"
    override val name = "Channel Breakout"
    override val description = "Donchian 20-period price breakout confirmed by volume"
    override val category = StrategyCategory.VOLATILITY

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 20) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Building 20-tick channel")
        }

        val (donchianHigh, donchianLow) = TechnicalIndicators.donchianChannels(prices.dropLast(1), 20)
        val currentPrice = data.currentPrice

        val isHighBreak = currentPrice >= donchianHigh
        val isLowBreak = currentPrice <= donchianLow

        val score = when {
            isHighBreak && data.orderFlowImbalance > 0.1 -> 0.85
            isLowBreak && data.orderFlowImbalance < -0.1 -> -0.85
            isHighBreak -> 0.50
            isLowBreak -> -0.50
            else -> 0.0
        }

        val signal = when {
            score > 0.6 -> SignalType.STRONG_BUY
            score > 0.2 -> SignalType.BUY
            score < -0.6 -> SignalType.STRONG_SELL
            score < -0.2 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = if (isHighBreak) "BREAKOUT HIGH! Price > ${"%.2f".format(donchianHigh)}"
        else if (isLowBreak) "BREAKDOWN LOW! Price < ${"%.2f".format(donchianLow)}"
        else "Inside channel [${"%.2f".format(donchianLow)} - ${"%.2f".format(donchianHigh)}]"

        return StrategyResult(id, name, signal, (0.5 + abs(score) * 0.45).coerceIn(0.0, 1.0), score, reason, mapOf("dHigh" to donchianHigh, "dLow" to donchianLow))
    }
}

/**
 * 10. VolumeProfileStrategy
 * Point of Control (POC) high volume node value area analysis.
 */
class VolumeProfileStrategy : Strategy {
    override val id = "volume_profile"
    override val name = "Volume Profile POC"
    override val description = "Point of Control high-volume node value area dynamics"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val trades = data.trades
        if (trades.size < 15) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Constructing Volume Profile")
        }

        val poc = TechnicalIndicators.pointOfControl(trades, 20)
        val currentPrice = data.currentPrice
        val pocDelta = (currentPrice - poc) / poc

        val score = when {
            pocDelta > 0.001 && data.orderFlowImbalance > 0.15 -> 0.70 // Trading above POC with aggressive buying
            pocDelta < -0.001 && data.orderFlowImbalance < -0.15 -> -0.70 // Trading below POC with aggressive selling
            else -> (pocDelta * 200.0).coerceIn(-0.4, 0.4)
        }

        val signal = when {
            score > 0.5 -> SignalType.STRONG_BUY
            score > 0.15 -> SignalType.BUY
            score < -0.5 -> SignalType.STRONG_SELL
            score < -0.15 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "POC: ${"%.2f".format(poc)}, Distance: ${"%.3f".format(pocDelta * 100)}%"
        return StrategyResult(id, name, signal, (0.55 + abs(score) * 0.4).coerceIn(0.0, 1.0), score, reason, mapOf("poc" to poc, "pocDelta" to pocDelta))
    }
}
