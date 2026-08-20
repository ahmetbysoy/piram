package com.example.domain.engine.strategy

import com.example.domain.model.ConsensusResult
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class StrategyEngine {

    val strategies: List<Strategy> = listOf(
        TrendFollowingStrategy(),
        MeanReversionStrategy(),
        MomentumStrategy(),
        VolumeSpikeStrategy(),
        RsiStrategy(),
        MacdStrategy(),
        BollingerBandsStrategy(),
        SupportResistanceStrategy(),
        BreakoutStrategy(),
        VolumeProfileStrategy(),
        DivergenceStrategy(),
        VolatilityBreakoutStrategy(),
        OrderFlowImbalanceStrategy(),
        MarketMicrostructureStrategy(),
        LiquidityHuntStrategy(),
        StatisticalArbitrageStrategy(),
        TimeBasedMomentumStrategy(),
        OrderBookPressureStrategy(),
        PriceActionStrategy(),
        BurstArbitrageStrategy()
    )

    private val enabledMap = ConcurrentHashMap<String, Boolean>().apply {
        strategies.forEach { put(it.id, true) }
    }

    fun isStrategyEnabled(id: String): Boolean = enabledMap[id] ?: true

    fun setStrategyEnabled(id: String, enabled: Boolean) {
        enabledMap[id] = enabled
    }

    fun toggleStrategy(id: String) {
        val current = isStrategyEnabled(id)
        enabledMap[id] = !current
    }

    fun executeAll(snapshot: MarketSnapshot): Pair<List<StrategyResult>, ConsensusResult> {
        val results = ArrayList<StrategyResult>(strategies.size)
        var weightedSum = 0.0
        var totalWeight = 0.0
        var bullishCount = 0
        var bearishCount = 0
        var neutralCount = 0

        var topBullish: StrategyResult? = null
        var topBearish: StrategyResult? = null

        for (strategy in strategies) {
            val isEnabled = isStrategyEnabled(strategy.id)
            if (!isEnabled) continue

            val result = strategy.execute(snapshot)
            results.add(result)

            val weight = result.confidence.coerceIn(0.1, 1.0)
            weightedSum += result.score * weight
            totalWeight += weight

            when (result.signal) {
                SignalType.STRONG_BUY, SignalType.BUY -> {
                    bullishCount++
                    if (topBullish == null || result.score > topBullish.score) {
                        topBullish = result
                    }
                }
                SignalType.STRONG_SELL, SignalType.SELL -> {
                    bearishCount++
                    if (topBearish == null || result.score < topBearish.score) {
                        topBearish = result
                    }
                }
                SignalType.NEUTRAL -> {
                    neutralCount++
                }
            }
        }

        val activeCount = results.size
        val normalizedScore = if (totalWeight > 0) (weightedSum / totalWeight).coerceIn(-1.0, 1.0) else 0.0
        val consensusStrength = normalizedScore * 100.0

        val (overallSignal, overallConfidence) = when {
            normalizedScore > 0.45 -> SignalType.STRONG_BUY to (0.70 + abs(normalizedScore) * 0.25)
            normalizedScore > 0.15 -> SignalType.BUY to (0.55 + abs(normalizedScore) * 0.25)
            normalizedScore < -0.45 -> SignalType.STRONG_SELL to (0.70 + abs(normalizedScore) * 0.25)
            normalizedScore < -0.15 -> SignalType.SELL to (0.55 + abs(normalizedScore) * 0.25)
            else -> SignalType.NEUTRAL to 0.50
        }

        val buyScore = ((normalizedScore + 1.0) / 2.0 * 100.0).coerceIn(0.0, 100.0)
        val sellScore = 100.0 - buyScore
        val neutralScore = if (activeCount > 0) (neutralCount.toDouble() / activeCount.toDouble()) * 100.0 else 0.0

        val consensus = ConsensusResult(
            overallSignal = overallSignal,
            buyScore = buyScore,
            sellScore = sellScore,
            neutralScore = neutralScore,
            confidence = overallConfidence.coerceIn(0.0, 1.0),
            consensusStrength = consensusStrength,
            activeStrategiesCount = activeCount,
            bullishCount = bullishCount,
            bearishCount = bearishCount,
            neutralCount = neutralCount,
            topBullishStrategy = topBullish?.name,
            topBearishStrategy = topBearish?.name
        )

        return Pair(results, consensus)
    }
}
