package com.example.domain.engine.strategy

import com.example.domain.model.MarketSnapshot
import com.example.domain.model.OrderSide
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 11. DivergenceStrategy
 * Price higher-high / lower-low divergence against RSI and Cumulative Volume Delta.
 */
class DivergenceStrategy : Strategy {
    override val id = "divergence"
    override val name = "Order Flow Divergence"
    override val description = "Price vs Cumulative Volume Delta & RSI momentum divergence"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        val trades = data.trades
        if (prices.size < 16 || trades.size < 16) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Synchronizing delta stream")
        }

        val half = prices.size / 2
        val firstHalfPrices = prices.take(half)
        val secondHalfPrices = prices.takeLast(half)

        val priceTrend = secondHalfPrices.last() - firstHalfPrices.last()
        val rsi = TechnicalIndicators.rsi(prices, 14)
        val flowImbalance = data.orderFlowImbalance

        // Bullish Divergence: Price falling but aggressive buy volume entering / RSI rising
        val isBullishDiv = priceTrend < 0 && flowImbalance > 0.25
        // Bearish Divergence: Price rising but aggressive sell volume hitting / RSI falling
        val isBearishDiv = priceTrend > 0 && flowImbalance < -0.25

        val score = when {
            isBullishDiv -> 0.80
            isBearishDiv -> -0.80
            else -> (flowImbalance * 0.4).coerceIn(-0.3, 0.3)
        }

        val signal = when {
            score > 0.6 -> SignalType.STRONG_BUY
            score > 0.2 -> SignalType.BUY
            score < -0.6 -> SignalType.STRONG_SELL
            score < -0.2 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = if (isBullishDiv) "BULLISH DELTA DIVERGENCE! Absorbing sell pressure."
        else if (isBearishDiv) "BEARISH DELTA DIVERGENCE! Distributing at highs."
        else "Flow and price aligned (Delta: ${"%.2f".format(flowImbalance * 100)}%)"

        return StrategyResult(id, name, signal, (0.55 + abs(score) * 0.4).coerceIn(0.0, 1.0), score, reason, mapOf("flowImbalance" to flowImbalance, "rsi" to rsi))
    }
}

/**
 * 12. VolatilityBreakoutStrategy
 * Realized volatility expansion ratio vs baseline standard deviation.
 */
class VolatilityBreakoutStrategy : Strategy {
    override val id = "volatility_breakout"
    override val name = "Volatility Expansion"
    override val description = "Standard deviation expansion ratio with directional pulse"
    override val category = StrategyCategory.VOLATILITY

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 20) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Measuring baseline volatility")
        }

        val shortStdDev = TechnicalIndicators.standardDeviation(prices.takeLast(6))
        val longStdDev = TechnicalIndicators.standardDeviation(prices.takeLast(20))

        val volRatio = if (longStdDev > 0) shortStdDev / longStdDev else 1.0
        val isExpansion = volRatio > 1.8

        val score = if (isExpansion) {
            val direction = if (prices.last() >= prices[prices.size - 6]) 1.0 else -1.0
            direction * (volRatio / 2.5).coerceIn(0.5, 0.95)
        } else {
            0.0
        }

        val signal = when {
            score > 0.55 -> SignalType.STRONG_BUY
            score > 0.15 -> SignalType.BUY
            score < -0.55 -> SignalType.STRONG_SELL
            score < -0.15 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "Vol Ratio: ${"%.2f".format(volRatio)}x [${if (isExpansion) "EXPLODING VOLATILITY" else "COMPRESSED"}]"
        return StrategyResult(id, name, signal, (0.5 + abs(score) * 0.45).coerceIn(0.0, 1.0), score, reason, mapOf("volRatio" to volRatio))
    }
}

/**
 * 13. OrderFlowImbalanceStrategy
 * High frequency Bid/Ask trade delta imbalance: (BuyVol - SellVol)/(BuyVol + SellVol).
 */
class OrderFlowImbalanceStrategy : Strategy {
    override val id = "order_flow_imbalance"
    override val name = "Order Flow Imbalance (OFI)"
    override val description = "Bid/Ask volume aggression ratio with whale weight"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val ofi = data.orderFlowImbalance
        val trades = data.trades
        val whaleCount = trades.count { it.isWhale }
        val whaleWeight = if (whaleCount > 0) 1.3 else 1.0

        val score = (ofi * whaleWeight).coerceIn(-1.0, 1.0)
        val signal = when {
            score > 0.45 -> SignalType.STRONG_BUY
            score > 0.15 -> SignalType.BUY
            score < -0.45 -> SignalType.STRONG_SELL
            score < -0.15 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "OFI Delta: ${"%.2f".format(ofi * 100)}%, Whale Trades: $whaleCount"
        return StrategyResult(id, name, signal, (0.6 + abs(score) * 0.35).coerceIn(0.0, 1.0), score, reason, mapOf("ofi" to ofi, "whaleCount" to whaleCount.toDouble()))
    }
}

/**
 * 14. MarketMicrostructureStrategy
 * Order book spread tightness, top-of-book depth ratio, and queue replenishment speed.
 */
class MarketMicrostructureStrategy : Strategy {
    override val id = "market_microstructure"
    override val name = "Market Microstructure"
    override val description = "Spread compression, book queue liquidity & replenishment"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val depth = data.depth
        if (depth == null || depth.bids.isEmpty() || depth.asks.isEmpty()) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Awaiting L2 Order Book depth")
        }

        val spreadPct = (depth.spread / depth.midPrice) * 10000.0 // in bps
        val topBidVol = depth.bids.take(5).sumOf { it.volume }
        val topAskVol = depth.asks.take(5).sumOf { it.volume }
        val bookImbalance = if (topBidVol + topAskVol > 0) (topBidVol - topAskVol) / (topBidVol + topAskVol) else 0.0

        val score = (bookImbalance * 1.1).coerceIn(-1.0, 1.0)
        val signal = when {
            score > 0.4 -> SignalType.STRONG_BUY
            score > 0.15 -> SignalType.BUY
            score < -0.4 -> SignalType.STRONG_SELL
            score < -0.15 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = "Spread: ${"%.2f".format(spreadPct)} bps, L2 Imbalance: ${"%.1f".format(bookImbalance * 100)}%"
        return StrategyResult(id, name, signal, (0.55 + abs(score) * 0.4).coerceIn(0.0, 1.0), score, reason, mapOf("spreadBps" to spreadPct, "bookImbalance" to bookImbalance))
    }
}

/**
 * 15. LiquidityHuntStrategy
 * Detects liquidity sweeps beyond recent local extrema with subsequent market absorption.
 */
class LiquidityHuntStrategy : Strategy {
    override val id = "liquidity_hunt"
    override val name = "Liquidity Sweep & Absorption"
    override val description = "Stop-hunt sweep detection at extremes followed by delta reversal"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val trades = data.trades
        if (trades.size < 20) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Mapping liquidity pools")
        }

        val high = trades.maxOf { it.price }
        val low = trades.minOf { it.price }
        val currentPrice = data.currentPrice
        val recent5 = trades.takeLast(5)
        val flow = data.orderFlowImbalance

        // Liquidity sweep low: price pierced near low but recent orders are heavy BUYs (absorption)
        val isLowSweep = (currentPrice - low) / (high - low + 0.0001) < 0.15 && flow > 0.35
        // Liquidity sweep high: price pierced near high but recent orders are heavy SELLs (distribution)
        val isHighSweep = (high - currentPrice) / (high - low + 0.0001) < 0.15 && flow < -0.35

        val score = when {
            isLowSweep -> 0.85 // Trapped sellers absorbed -> Launch UP
            isHighSweep -> -0.85 // Trapped buyers distributed -> Dump DOWN
            else -> 0.0
        }

        val signal = when {
            score > 0.6 -> SignalType.STRONG_BUY
            score > 0.2 -> SignalType.BUY
            score < -0.6 -> SignalType.STRONG_SELL
            score < -0.2 -> SignalType.SELL
            else -> SignalType.NEUTRAL
        }

        val reason = if (isLowSweep) "LIQUIDITY SWEPT AT LOWS! Massive absorption."
        else if (isHighSweep) "LIQUIDITY SWEPT AT HIGHS! Aggressive dump."
        else "No sweep detected in current range."

        return StrategyResult(id, name, signal, (0.55 + abs(score) * 0.4).coerceIn(0.0, 1.0), score, reason, mapOf("isLowSweep" to (if (isLowSweep) 1.0 else 0.0)))
    }
}
