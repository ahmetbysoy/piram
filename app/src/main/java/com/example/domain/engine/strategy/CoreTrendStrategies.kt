package com.example.domain.engine.strategy

import com.example.domain.model.MarketSnapshot
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import kotlin.math.abs

/**
 * 1. TrendFollowingStrategy
 * Uses EMA fast (9) / slow (21) cross, price vs VWAP relationship and trade aggression.
 */
class TrendFollowingStrategy : Strategy {
    override val id = "trend_following"
    override val name = "Trend Following"
    override val description = "EMA 9/21 cross coupled with VWAP slope and order flow bias"
    override val category = StrategyCategory.TREND

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 21) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Building price buffer (${prices.size}/21)")
        }

        val ema9 = TechnicalIndicators.ema(prices, 9)
        val ema21 = TechnicalIndicators.ema(prices, 21)
        val vwap = if (data.vwap > 0) data.vwap else prices.last()
        val currentPrice = data.currentPrice

        val emaDiff = (ema9 - ema21) / ema21
        val vwapDiff = (currentPrice - vwap) / vwap
        val flowBias = data.orderFlowImbalance

        val combinedScore = (emaDiff * 150.0).coerceIn(-1.0, 1.0) * 0.45 +
                (vwapDiff * 100.0).coerceIn(-1.0, 1.0) * 0.35 +
                flowBias * 0.20

        val (signal, confidence) = when {
            combinedScore > 0.45 -> SignalType.STRONG_BUY to (0.75 + abs(combinedScore) * 0.2)
            combinedScore > 0.15 -> SignalType.BUY to (0.60 + abs(combinedScore) * 0.2)
            combinedScore < -0.45 -> SignalType.STRONG_SELL to (0.75 + abs(combinedScore) * 0.2)
            combinedScore < -0.15 -> SignalType.SELL to (0.60 + abs(combinedScore) * 0.2)
            else -> SignalType.NEUTRAL to 0.50
        }

        val reason = "EMA9: ${"%.2f".format(ema9)}, EMA21: ${"%.2f".format(ema21)}, VWAP: ${"%.2f".format(vwap)}"
        return StrategyResult(id, name, signal, confidence.coerceIn(0.0, 1.0), combinedScore, reason, mapOf("ema9" to ema9, "ema21" to ema21, "vwap" to vwap))
    }
}

/**
 * 2. MeanReversionStrategy
 * Uses Bollinger %B and price Z-score distance from 20-period moving average.
 */
class MeanReversionStrategy : Strategy {
    override val id = "mean_reversion"
    override val name = "Mean Reversion"
    override val description = "Statistical price deviation from mean with Bollinger envelope"
    override val category = StrategyCategory.MOMENTUM

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 20) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Insufficient sample for mean calculation")
        }

        val (upper, middle, lower) = TechnicalIndicators.bollingerBands(prices, 20, 2.0)
        val bandWidth = upper - lower
        val currentPrice = data.currentPrice

        val percentB = if (bandWidth > 0) (currentPrice - lower) / bandWidth else 0.5
        val zScore = (currentPrice - middle) / (if (bandWidth > 0) bandWidth / 4.0 else 1.0)

        // Mean reversion expects pullback when extreme
        val score = when {
            percentB > 1.05 || zScore > 2.2 -> -0.85 // Extreme overbought -> Mean reversion SELL
            percentB > 0.95 -> -0.45
            percentB < -0.05 || zScore < -2.2 -> 0.85 // Extreme oversold -> Mean reversion BUY
            percentB < 0.05 -> 0.45
            else -> -(percentB - 0.5) * 0.5
        }

        val signal = when {
            score > 0.6 -> SignalType.STRONG_BUY
            score > 0.2 -> SignalType.BUY
            score < -0.6 -> SignalType.STRONG_SELL
            score < -0.2 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "Band %B: ${"%.2f".format(percentB * 100)}%, Z-Score: ${"%.2f".format(zScore)}"
        return StrategyResult(id, name, signal, (0.55 + abs(score) * 0.4).coerceIn(0.0, 1.0), score, reason, mapOf("percentB" to percentB, "zScore" to zScore))
    }
}

/**
 * 3. MomentumStrategy
 * Calculates Rate of Change (ROC) and rapid price acceleration.
 */
class MomentumStrategy : Strategy {
    override val id = "momentum"
    override val name = "Momentum Surge"
    override val description = "Rate of Change and directional velocity acceleration"
    override val category = StrategyCategory.MOMENTUM

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 12) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Collecting momentum tick stream")
        }

        val rocShort = TechnicalIndicators.rateOfChange(prices, 5)
        val rocMed = TechnicalIndicators.rateOfChange(prices, 10)
        val weightedRoc = rocShort * 0.6 + rocMed * 0.4

        val score = (weightedRoc / 0.8).coerceIn(-1.0, 1.0)
        val signal = when {
            score > 0.5 -> SignalType.STRONG_BUY
            score > 0.15 -> SignalType.BUY
            score < -0.5 -> SignalType.STRONG_SELL
            score < -0.15 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "ROC(5): ${"%.3f".format(rocShort)}%, ROC(10): ${"%.3f".format(rocMed)}%"
        return StrategyResult(id, name, signal, (0.5 + abs(score) * 0.45).coerceIn(0.0, 1.0), score, reason, mapOf("rocShort" to rocShort, "rocMed" to rocMed))
    }
}

/**
 * 4. VolumeSpikeStrategy
 * Identifies sudden volume anomalies using Z-score with trade direction confirmation.
 */
class VolumeSpikeStrategy : Strategy {
    override val id = "volume_spike"
    override val name = "Volume Spike"
    override val description = "High statistical volume anomaly with buy/sell aggression"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val trades = data.trades
        if (trades.size < 10) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Waiting for trade buffer")
        }

        val recentVols = trades.takeLast(30).map { it.volume }
        val currentVol = recentVols.takeLast(3).sum()
        val zScore = TechnicalIndicators.volumeZScore(currentVol, recentVols)
        val flowDelta = data.orderFlowImbalance

        val isSpike = zScore > 1.8
        val score = if (isSpike) {
            (flowDelta * 1.2).coerceIn(-1.0, 1.0)
        } else {
            flowDelta * 0.25
        }

        val signal = when {
            score > 0.55 -> SignalType.STRONG_BUY
            score > 0.15 -> SignalType.BUY
            score < -0.55 -> SignalType.STRONG_SELL
            score < -0.15 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = if (isSpike) "SPIKE DETECTED! Z: ${"%.2f".format(zScore)}, Delta: ${"%.2f".format(flowDelta * 100)}%" else "Normal volume. Z: ${"%.2f".format(zScore)}"
        return StrategyResult(id, name, signal, (0.5 + abs(score) * 0.45).coerceIn(0.0, 1.0), score, reason, mapOf("volumeZ" to zScore, "imbalance" to flowDelta))
    }
}

/**
 * 5. RsiStrategy
 * Classical 14-period Relative Strength Index with dynamic momentum thresholding.
 */
class RsiStrategy : Strategy {
    override val id = "rsi"
    override val name = "RSI Momentum"
    override val description = "Wilder's smoothed 14-period Relative Strength Index"
    override val category = StrategyCategory.MOMENTUM

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 15) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Gathering RSI sample")
        }

        val rsiVal = TechnicalIndicators.rsi(prices, 14)
        val score = when {
            rsiVal < 25.0 -> 0.85 // Deeply oversold -> Strong Buy
            rsiVal < 35.0 -> 0.45
            rsiVal > 75.0 -> -0.85 // Deeply overbought -> Strong Sell
            rsiVal > 65.0 -> -0.45
            else -> -((rsiVal - 50.0) / 25.0) * 0.2
        }

        val signal = when {
            score > 0.6 -> SignalType.STRONG_BUY
            score > 0.2 -> SignalType.BUY
            score < -0.6 -> SignalType.STRONG_SELL
            score < -0.2 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "RSI(14): ${"%.1f".format(rsiVal)} [${if (rsiVal > 70) "OVERBOUGHT" else if (rsiVal < 30) "OVERSOLD" else "BALANCED"}]"
        return StrategyResult(id, name, signal, (0.5 + abs(score) * 0.45).coerceIn(0.0, 1.0), score, reason, mapOf("rsi" to rsiVal))
    }
}
