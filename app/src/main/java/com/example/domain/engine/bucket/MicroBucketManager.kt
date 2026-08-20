package com.example.domain.engine.bucket

import com.example.core.util.MathUtils
import com.example.domain.model.LayerAggregate
import com.example.domain.model.Order

class MicroBucketManager(
    val numLayers: Int = 8,
    minVolume: Double = 0.001,
    maxVolume: Double = 50.0
) {
    private var thresholds: DoubleArray = MathUtils.createLogarithmicThresholds(minVolume, maxVolume, numLayers)
    private val buckets: Array<MicroBucket>

    init {
        buckets = Array(numLayers) { i ->
            val minV = thresholds[i]
            val maxV = thresholds[i + 1]
            val label = when (i) {
                0 -> "Micro (<${MathUtils.formatVolume(maxV)})"
                numLayers - 1 -> "Whale (>${MathUtils.formatVolume(minV)})"
                numLayers - 2 -> "Shark (${MathUtils.formatVolume(minV)}-${MathUtils.formatVolume(maxV)})"
                else -> "L${i + 1} (${MathUtils.formatVolume(minV)}-${MathUtils.formatVolume(maxV)})"
            }
            MicroBucket(i, minV, maxV, label)
        }
    }

    fun reconfigureThresholds(minVolume: Double, maxVolume: Double) {
        thresholds = MathUtils.createLogarithmicThresholds(minVolume, maxVolume, numLayers)
    }

    fun processOrder(order: Order): Order {
        val bucketIdx = MathUtils.findBucketIndex(order.volume, thresholds)
        val isWhale = bucketIdx >= numLayers - 2
        buckets[bucketIdx].addOrder(order)
        return order.copy(layerIndex = bucketIdx, isWhale = isWhale)
    }

    fun decayAll(decayRate: Float = 0.15f, dtSeconds: Float = 0.1f) {
        for (bucket in buckets) {
            bucket.decay(decayRate, dtSeconds)
        }
    }

    fun updateDisplay(smoothingFactor: Float = 0.2f) {
        for (bucket in buckets) {
            bucket.updateDisplay(smoothingFactor)
        }
    }

    fun getAggregatedLayers(): List<LayerAggregate> {
        return buckets.mapIndexed { idx, bucket ->
            bucket.toAggregate(isWhaleTier = idx >= numLayers - 2)
        }
    }

    fun getWhaleVolume(): Double {
        var whaleSum = 0.0
        for (i in (numLayers - 2) until numLayers) {
            whaleSum += buckets[i].currentVolume
        }
        return whaleSum
    }

    fun getRetailVolume(): Double {
        var retailSum = 0.0
        for (i in 0 until (numLayers - 2)) {
            retailSum += buckets[i].currentVolume
        }
        return retailSum
    }

    fun reset() {
        for (bucket in buckets) {
            bucket.reset()
        }
    }
}
