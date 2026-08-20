package com.example.domain.engine.bucket

import com.example.core.util.MathUtils
import com.example.domain.engine.SignalConfig
import com.example.domain.model.LayerAggregate
import com.example.domain.model.Order

/**
 * Notional (USDT) mikro-katman yöneticisi.
 * Eşikler logaritmik; varsayılan aralık [MIN_NOTIONAL, MAX_NOTIONAL] = 100 .. 1M USDT.
 */
class MicroBucketManager(
    val numLayers: Int = SignalConfig.DEFAULT_LAYERS,
    minNotional: Double = SignalConfig.MIN_NOTIONAL,
    maxNotional: Double = SignalConfig.MAX_NOTIONAL
) {
    private var thresholds: DoubleArray = MathUtils.createLogarithmicThresholds(minNotional, maxNotional, numLayers)
    private val buckets: Array<MicroBucket>

    init {
        buckets = Array(numLayers) { i ->
            val minV = thresholds[i]
            val maxV = thresholds[i + 1]
            val label = when (i) {
                0 -> "Micro (<${MathUtils.formatUsd(maxV)})"
                numLayers - 1 -> "Whale (>${MathUtils.formatUsd(minV)})"
                numLayers - 2 -> "Shark (${MathUtils.formatUsd(minV)}-${MathUtils.formatUsd(maxV)})"
                else -> "L${i + 1} (${MathUtils.formatUsd(minV)}-${MathUtils.formatUsd(maxV)})"
            }
            MicroBucket(i, minV, maxV, label)
        }
    }

    fun reconfigureThresholds(minNotional: Double, maxNotional: Double) {
        thresholds = MathUtils.createLogarithmicThresholds(minNotional, maxNotional, numLayers)
    }

    /** Gelen siparişi notional (USDT) değerine göre katmana atar; whale bayrağını işler. */
    fun processOrder(order: Order): Order {
        val bucketIdx = MathUtils.findBucketIndex(order.value, thresholds)
        val isWhale = bucketIdx >= numLayers - 2
        buckets[bucketIdx].addOrder(order)
        return order.copy(layerIndex = bucketIdx, isWhale = isWhale)
    }

    fun decayAll(decayRate: Float = SignalConfig.DEFAULT_DECAY, dtSeconds: Float = 0.1f) {
        for (bucket in buckets) {
            bucket.decay(decayRate, dtSeconds)
        }
    }

    fun updateDisplay(smoothingFactor: Float = SignalConfig.DISPLAY_SMOOTHING) {
        for (bucket in buckets) {
            bucket.updateDisplay(smoothingFactor)
        }
    }

    fun getAggregatedLayers(): List<LayerAggregate> {
        return buckets.mapIndexed { idx, bucket ->
            bucket.toAggregate(isWhaleTier = idx >= numLayers - 2)
        }
    }

    /** Üst 2 katmanın (whale + shark) toplam notional'i. */
    fun getWhaleNotional(): Double {
        var whaleSum = 0.0
        for (i in (numLayers - 2) until numLayers) {
            whaleSum += buckets[i].currentNotional
        }
        return whaleSum
    }

    fun getRetailNotional(): Double {
        var retailSum = 0.0
        for (i in 0 until (numLayers - 2)) {
            retailSum += buckets[i].currentNotional
        }
        return retailSum
    }

    fun reset() {
        for (bucket in buckets) {
            bucket.reset()
        }
    }
}
