package com.example.domain.engine.strategy

import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object TechnicalIndicators {

    fun sma(values: List<Double>, period: Int): Double {
        if (values.isEmpty() || period <= 0) return 0.0
        val sample = values.takeLast(period)
        return sample.sum() / sample.size
    }

    fun ema(values: List<Double>, period: Int): Double {
        if (values.isEmpty() || period <= 0) return 0.0
        if (values.size == 1) return values.first()
        val k = 2.0 / (period + 1.0)
        var emaVal = values.first()
        for (i in 1 until values.size) {
            emaVal = values[i] * k + emaVal * (1.0 - k)
        }
        return emaVal
    }

    fun emaSeries(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty() || period <= 0) return emptyList()
        val k = 2.0 / (period + 1.0)
        val result = ArrayList<Double>(values.size)
        var currentEma = values.first()
        result.add(currentEma)
        for (i in 1 until values.size) {
            currentEma = values[i] * k + currentEma * (1.0 - k)
            result.add(currentEma)
        }
        return result
    }

    fun rsi(prices: List<Double>, period: Int = 14): Double {
        if (prices.size <= period) return 50.0

        var avgGain = 0.0
        var avgLoss = 0.0

        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= period
        avgLoss /= period

        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    /**
     * Returns Triple(macdLine, signalLine, histogram)
     */
    fun macd(
        prices: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<Double, Double, Double> {
        if (prices.size < slowPeriod) return Triple(0.0, 0.0, 0.0)

        val fastEmaSeries = emaSeries(prices, fastPeriod)
        val slowEmaSeries = emaSeries(prices, slowPeriod)

        val macdSeries = fastEmaSeries.zip(slowEmaSeries) { fast, slow -> fast - slow }
        val signalSeries = emaSeries(macdSeries, signalPeriod)

        val macdLine = macdSeries.lastOrNull() ?: 0.0
        val signalLine = signalSeries.lastOrNull() ?: 0.0
        val histogram = macdLine - signalLine

        return Triple(macdLine, signalLine, histogram)
    }

    /**
     * Returns Triple(upperBand, middleBand, lowerBand)
     */
    fun bollingerBands(
        prices: List<Double>,
        period: Int = 20,
        multiplier: Double = 2.0
    ): Triple<Double, Double, Double> {
        if (prices.isEmpty()) return Triple(0.0, 0.0, 0.0)
        val sample = prices.takeLast(min(period, prices.size))
        val middle = sample.sum() / sample.size
        val variance = sample.map { (it - middle).pow(2) }.sum() / sample.size
        val stdDev = sqrt(variance)
        val upper = middle + multiplier * stdDev
        val lower = middle - multiplier * stdDev
        return Triple(upper, middle, lower)
    }

    fun vwap(trades: List<Order>): Double {
        if (trades.isEmpty()) return 0.0
        var cumulativeTotal = 0.0
        var cumulativeVolume = 0.0
        for (trade in trades) {
            cumulativeTotal += trade.price * trade.volume
            cumulativeVolume += trade.volume
        }
        return if (cumulativeVolume > 0.0) cumulativeTotal / cumulativeVolume else 0.0
    }

    fun orderFlowImbalance(trades: List<Order>): Double {
        if (trades.isEmpty()) return 0.0
        var buyVol = 0.0
        var sellVol = 0.0
        for (trade in trades) {
            if (trade.side == OrderSide.BUY) buyVol += trade.volume else sellVol += trade.volume
        }
        val total = buyVol + sellVol
        return if (total > 0) (buyVol - sellVol) / total else 0.0
    }

    fun volumeZScore(currentVolume: Double, recentVolumes: List<Double>): Double {
        if (recentVolumes.size <= 2) return 0.0
        val mean = recentVolumes.sum() / recentVolumes.size
        val variance = recentVolumes.map { (it - mean).pow(2) }.sum() / (recentVolumes.size - 1)
        val stdDev = sqrt(variance)
        return if (stdDev > 0) (currentVolume - mean) / stdDev else 0.0
    }

    fun donchianChannels(prices: List<Double>, period: Int = 20): Pair<Double, Double> {
        if (prices.isEmpty()) return Pair(0.0, 0.0)
        val sample = prices.takeLast(min(period, prices.size))
        return Pair(sample.maxOrNull() ?: prices.last(), sample.minOrNull() ?: prices.last())
    }

    fun pointOfControl(trades: List<Order>, bins: Int = 20): Double {
        if (trades.isEmpty()) return 0.0
        val minPrice = trades.minOf { it.price }
        val maxPrice = trades.maxOf { it.price }
        if (minPrice == maxPrice) return minPrice

        val binStep = (maxPrice - minPrice) / bins
        val binVolumes = DoubleArray(bins)

        for (trade in trades) {
            val binIdx = ((trade.price - minPrice) / binStep).toInt().coerceIn(0, bins - 1)
            binVolumes[binIdx] += trade.volume
        }

        var maxVolIdx = 0
        var maxVol = -1.0
        for (i in binVolumes.indices) {
            if (binVolumes[i] > maxVol) {
                maxVol = binVolumes[i]
                maxVolIdx = i
            }
        }
        return minPrice + (maxVolIdx + 0.5) * binStep
    }

    fun standardDeviation(values: List<Double>): Double {
        if (values.size <= 1) return 0.0
        val mean = values.sum() / values.size
        val variance = values.map { (it - mean).pow(2) }.sum() / (values.size - 1)
        return sqrt(variance)
    }

    fun rateOfChange(prices: List<Double>, period: Int = 10): Double {
        if (prices.size <= period) return 0.0
        val prev = prices[prices.size - 1 - period]
        val current = prices.last()
        return if (prev > 0) ((current - prev) / prev) * 100.0 else 0.0
    }
}
